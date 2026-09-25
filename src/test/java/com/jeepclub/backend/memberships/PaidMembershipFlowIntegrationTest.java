package com.jeepclub.backend.memberships;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.core.read.ListAppender;
import com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType;
import com.jeepclub.backend.billing.core.domain.enums.charge.MemberChargeStatus;
import com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy;
import com.jeepclub.backend.billing.core.domain.model.ChargeDefinition;
import com.jeepclub.backend.billing.core.domain.model.ChargeCycle;
import com.jeepclub.backend.billing.core.domain.model.MemberCharge;
import com.jeepclub.backend.billing.core.application.query.MembershipChargeQueryService;
import com.jeepclub.backend.billing.core.repository.ChargeCycleRepository;
import com.jeepclub.backend.billing.core.repository.ChargeDefinitionRepository;
import com.jeepclub.backend.billing.core.repository.MemberChargeRepository;
import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.memberships.api.security.RequiresMembership;
import com.jeepclub.backend.memberships.core.application.query.MembershipAccessQueryService;
import com.jeepclub.backend.memberships.core.application.service.membershipbilling.AdminMembershipBillingConfigurationService;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.JwtAuthenticatedUser;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import({
        PaidMembershipFlowIntegrationTest.TestController.class,
        PaidMembershipFlowIntegrationTest.FixedClockConfiguration.class
})
class PaidMembershipFlowIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-09-21T12:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 21);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ChargeDefinitionRepository definitionRepository;
    @Autowired
    private ChargeCycleRepository cycleRepository;
    @Autowired
    private MemberChargeRepository memberChargeRepository;
    @Autowired
    private AdminMembershipBillingConfigurationService configurationService;
    @MockitoSpyBean
    private MembershipChargeQueryService membershipChargeQueryService;
    @MockitoBean
    private JwtTokenParser jwtTokenParser;
    @MockitoBean
    private UserAuthoritiesProvider userAuthoritiesProvider;
    @MockitoBean
    private AccessTokenAuthenticationService accessTokenAuthenticationService;

    @Test
    void shouldAllowProtectedEndpointWithoutConfiguration() throws Exception {
        authenticate("no-config", List.of());

        performProtected("no-config").andExpect(status().isOk());
    }

    @Test
    void shouldAllowWhenEnforcementIsDisabled() throws Exception {
        Long definitionId = configure(false).getId();
        authenticate("disabled", List.of());

        performProtected("disabled").andExpect(status().isOk());
        assertThat(definitionId).isPositive();
    }

    @ParameterizedTest
    @EnumSource(value = MemberChargeStatus.class)
    void shouldAllowPersistedPendingPaidAndCanceledStates(MemberChargeStatus status) throws Exception {
        ChargeDefinition definition = configure(true);
        ChargeCycle cycle = cycleRepository.save(cycle(definition, Scenario.ALLOWED));
        memberChargeRepository.save(charge(definition.getId(), cycle.getId(), status, Scenario.ALLOWED));
        authenticate("allowed-" + status, List.of());

        performProtected("allowed-" + status).andExpect(status().isOk());
    }

    @ParameterizedTest
    @EnumSource(value = Scenario.class, names = {"OVERDUE", "EXPIRED"})
    void shouldReturnPaymentRequiredForEffectiveBlockingStates(Scenario scenario) throws Exception {
        ChargeDefinition definition = configure(true);
        ChargeCycle cycle = cycleRepository.save(cycle(definition, scenario));
        memberChargeRepository.save(charge(definition.getId(), cycle.getId(), MemberChargeStatus.PENDING, scenario));
        authenticate("blocked-" + scenario, List.of());

        performProtected("blocked-" + scenario)
                .andExpect(status().isPaymentRequired())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MEMBERSHIP_PAYMENT_REQUIRED"));
    }

    @Test
    void shouldReturnOperationalErrorAndWarnWhenExpectedChargeIsMissing() throws Exception {
        ChargeDefinition definition = configure(true);
        cycleRepository.save(cycle(definition, Scenario.ALLOWED));
        authenticate("missing", List.of());
        Logger logger = (Logger) LoggerFactory.getLogger(MembershipAccessQueryService.class);
        ListAppender<ch.qos.logback.classic.spi.ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            performProtected("missing")
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.code").value("MEMBERSHIP_CHARGE_UNAVAILABLE"));

            assertThat(appender.list)
                    .anySatisfy(event -> assertThat(event.getLevel()).isEqualTo(Level.WARN));
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void shouldTranslateUnexpectedBillingFailureThroughCompleteHttpPipeline() throws Exception {
        ChargeDefinition definition = configure(true);
        authenticate("billing-failure", List.of());
        doThrow(new RuntimeException("sensitive database detail"))
                .when(membershipChargeQueryService).evaluate(definition.getId(), 42L);

        try {
            String body = performProtected("billing-failure")
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.code").value("MEMBERSHIP_CHARGE_UNAVAILABLE"))
                    .andReturn().getResponse().getContentAsString();

            assertThat(body)
                    .doesNotContain("sensitive database detail")
                    .doesNotContain("RuntimeException")
                    .doesNotContain("stackTrace");
        } finally {
            reset(membershipChargeQueryService);
        }
    }

    @Test
    void shouldEnforceAdministrativeConfigurationAuthorities() throws Exception {
        mockMvc.perform(get("/admin/membership-billing-configuration"))
                .andExpect(status().isUnauthorized());

        authenticate("without-permission", List.of());
        mockMvc.perform(get("/admin/membership-billing-configuration")
                        .header("Authorization", "Bearer without-permission"))
                .andExpect(status().isForbidden());

        authenticate("reader", List.of("MEMBERSHIP_BILLING_CONFIGURATION_READ"));
        mockMvc.perform(get("/admin/membership-billing-configuration")
                        .header("Authorization", "Bearer reader"))
                .andExpect(status().isNoContent());

        ChargeDefinition definition = definitionRepository.save(activeDefinition());
        authenticate("updater", List.of("MEMBERSHIP_BILLING_CONFIGURATION_UPDATE"));
        mockMvc.perform(put("/admin/membership-billing-configuration")
                        .header("Authorization", "Bearer updater")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chargeDefinitionId\":" + definition.getId() + ",\"enforcementEnabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chargeDefinitionId").value(definition.getId()))
                .andExpect(jsonPath("$.enforcementEnabled").value(true));
    }

    private org.springframework.test.web.servlet.ResultActions performProtected(String token) throws Exception {
        return mockMvc.perform(get("/test-paid-membership/protected")
                .header("Authorization", "Bearer " + token));
    }

    private ChargeDefinition configure(boolean enabled) {
        ChargeDefinition definition = definitionRepository.save(activeDefinition());
        configurationService.configure(definition.getId(), enabled);
        return definition;
    }

    private static ChargeDefinition activeDefinition() {
        return ChargeDefinition.create(
                "Membership " + System.nanoTime(),
                "Annual club membership",
                BigDecimal.valueOf(100),
                ChargeRecurrenceType.YEARLY,
                true,
                PaymentAcceptancePolicy.AFTER_DUE_DATE,
                null,
                NOW
        );
    }

    private static MemberCharge charge(
            Long definitionId,
            Long cycleId,
            MemberChargeStatus status,
            Scenario scenario
    ) {
        LocalDate dueDate = switch (scenario) {
            case ALLOWED -> TODAY.plusDays(1);
            case OVERDUE, EXPIRED -> TODAY.minusDays(1);
        };
        PaymentAcceptancePolicy policy = scenario == Scenario.EXPIRED
                ? PaymentAcceptancePolicy.UNTIL_DUE_DATE
                : PaymentAcceptancePolicy.AFTER_DUE_DATE;
        LocalDate paymentAllowedUntil = scenario == Scenario.EXPIRED ? dueDate : null;

        return MemberCharge.reconstitute(
                null,
                42L,
                definitionId,
                cycleId,
                BigDecimal.valueOf(100),
                BigDecimal.valueOf(100),
                dueDate,
                policy,
                null,
                paymentAllowedUntil,
                status,
                NOW.minusSeconds(60),
                NOW,
                status == MemberChargeStatus.PAID ? NOW : null,
                status == MemberChargeStatus.CANCELED ? NOW : null
        );
    }

    private static ChargeCycle cycle(ChargeDefinition definition, Scenario scenario) {
        LocalDate dueDate = switch (scenario) {
            case ALLOWED -> TODAY.plusDays(1);
            case OVERDUE, EXPIRED -> TODAY.minusDays(1);
        };
        return ChargeCycle.generate(
                definition,
                "membership-" + System.nanoTime(),
                dueDate,
                1L,
                NOW.minusSeconds(120)
        );
    }

    private void authenticate(String token, List<String> authorities) {
        when(jwtTokenParser.parseAndValidate(token)).thenReturn(
                new JwtAuthenticatedUser(42L, 49L, "Member", NOW.plusSeconds(3600))
        );
        when(userAuthoritiesProvider.findAuthorityCodesByUserId(42L)).thenReturn(authorities);
    }

    private enum Scenario {
        ALLOWED,
        OVERDUE,
        EXPIRED
    }

    @RestController
    static class TestController {
        @RequiresMembership
        @GetMapping("/test-paid-membership/protected")
        String protectedEndpoint() {
            return "ok";
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {
        @Bean
        @Primary
        Clock fixedMembershipClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
