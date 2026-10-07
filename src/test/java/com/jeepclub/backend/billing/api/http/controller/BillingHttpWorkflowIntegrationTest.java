package com.jeepclub.backend.billing.api.http.controller;

import com.jeepclub.backend.billing.core.repository.*;
import com.jeepclub.backend.billing.core.domain.model.*;
import com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy;
import com.jeepclub.backend.iam.authentication.core.application.service.security.AccessTokenAuthenticationService;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.*;
import com.jeepclub.backend.billing.core.port.*;
import com.jeepclub.backend.shared.storage.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.time.*;
import java.util.*;
import static com.jeepclub.backend.billing.support.BillingFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
@Import(BillingHttpWorkflowIntegrationTest.TimeConfiguration.class)
@Transactional
class BillingHttpWorkflowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired ChargeDefinitionRepository definitions;
    @Autowired ChargeCycleRepository cycles;
    @Autowired MemberChargeRepository charges;
    @Autowired MemberPaymentRepository payments;
    @Autowired MemberRefundRepository refunds;
    @MockitoBean JwtTokenParser jwtTokenParser;
    @MockitoBean UserAuthoritiesProvider authoritiesProvider;
    @MockitoBean AccessTokenAuthenticationService authenticationService;
    @MockitoBean BillingMembershipPort members;
    @MockitoBean BillingAuthorizationPort roles;
    @MockitoBean BillingEventPort events;
    @MockitoBean FileStorage storage;
    @TestConfiguration static class TimeConfiguration {
        @Bean @Primary Clock billingTestClock() { return CLOCK; }
        @Bean org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer billingValidationClock() {
            return configuration -> configuration.clockProvider(() -> CLOCK);
        }
    }
    static final String DEFINITION = """
        {"name":"fee","description":"annual","defaultAmount":100,"recurrenceType":"ONE_TIME","required":true,"paymentAcceptancePolicy":"AFTER_DUE_DATE"}
        """;
    void auth(long user, String... permissions) {
        when(jwtTokenParser.parseAndValidate("billing-token")).thenReturn(new JwtAuthenticatedUser(user, user + 1000, "Billing user", NOW.plusSeconds(3600)));
        when(authoritiesProvider.findAuthorityCodesByUserId(user)).thenReturn(List.of(permissions));
    }
    static <T extends AbstractMockHttpServletRequestBuilder<T>> T token(T request) { return request.header("Authorization", "Bearer billing-token"); }
    static MockHttpServletRequestBuilder body(MockHttpServletRequestBuilder request, String body) { return token(request.contentType("application/json").content(body)); }
    long id(MvcResult result) throws Exception { return json.readTree(result.getResponse().getContentAsString()).get("id").asLong(); }
    MemberCharge seedCharge() {
        var d = definitions.save(ChargeDefinition.create("seed", null, AMOUNT, com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType.ONE_TIME,
                true, PaymentAcceptancePolicy.AFTER_DUE_DATE, null, NOW));
        var cycle = cycles.save(ChargeCycle.generate(d, "seed-cycle", DUE, 99L, NOW));
        return charges.save(MemberCharge.create(10L, d.getId(), cycle.getId(), AMOUNT, DUE, PaymentAcceptancePolicy.AFTER_DUE_DATE, null, NOW));
    }
    @Test void definitionAssignmentGenerationAndAdministrativeLifecycleArePubliclyConsistent() throws Exception {
        auth(99L, "BILLING_CHARGE_DEFINITION_CREATE", "BILLING_CHARGE_DEFINITION_UPDATE", "BILLING_CHARGE_DEFINITION_READ",
                "BILLING_CHARGE_ASSIGNMENT_CREATE", "BILLING_CHARGE_ASSIGNMENT_UPDATE", "BILLING_CHARGE_ASSIGNMENT_READ",
                "BILLING_CHARGE_CYCLE_GENERATE", "BILLING_CHARGE_CYCLE_READ", "BILLING_CHARGE_CYCLE_FINISH", "BILLING_CHARGE_CYCLE_ARCHIVE");
        long definition = id(mvc.perform(body(post("/billing/charge-definitions"), DEFINITION)).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("ACTIVE")).andReturn());
        when(members.existsActiveMemberByUserId(10L)).thenReturn(true);
        long assignment = id(mvc.perform(token(post("/billing/charge-definitions/{id}/assignments/users/10", definition))).andExpect(status().isCreated()).andExpect(jsonPath("$.userId").value(10)).andReturn());
        mvc.perform(token(post("/billing/charge-definitions/{id}/assignments/users/10", definition))).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CHARGE_ASSIGNMENT_ALREADY_EXISTS"));
        mvc.perform(token(get("/billing/charge-definitions/{id}/assignments", definition))).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(assignment));
        var generated = mvc.perform(body(post("/billing/charge-definitions/{id}/cycles", definition), "{\"code\":\"SEP\",\"dueDate\":\"2026-09-15\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.createdMemberCharges").value(1)).andReturn();
        long cycle = json.readTree(generated.getResponse().getContentAsString()).get("chargeCycle").get("id").asLong();
        mvc.perform(token(get("/billing/charge-cycles/{id}", cycle))).andExpect(status().isOk());
        mvc.perform(token(get("/billing/charge-definitions/{id}/cycles", definition))).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(cycle));
        mvc.perform(token(patch("/billing/charge-cycles/{id}/finish", cycle))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FINISHED"));
        mvc.perform(token(patch("/billing/charge-cycles/{id}/archive", cycle))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ARCHIVED"));
        mvc.perform(token(patch("/billing/charge-definitions/{id}/deactivate", definition))).andExpect(status().isOk());
        mvc.perform(token(patch("/billing/charge-definitions/{id}/activate", definition))).andExpect(status().isOk());
        mvc.perform(body(put("/billing/charge-definitions/{id}", definition), DEFINITION.replace("annual", "changed"))).andExpect(status().isOk()).andExpect(jsonPath("$.description").value("changed"));
        mvc.perform(token(patch("/billing/charge-definitions/{id}/archive", definition))).andExpect(status().isOk());
        mvc.perform(token(patch("/billing/charge-definitions/{id}/activate", definition))).andExpect(status().isConflict());
        assertThat(cycles.findById(cycle).orElseThrow().getChargeDefinitionDescriptionSnapshot()).isEqualTo("annual");
        assertThat(charges.findByChargeCycleId(cycle)).allMatch(MemberCharge::isPending);
    }
    MockMultipartHttpServletRequestBuilder upload(String route, long id, boolean update) {
        var builder = multipart(route, id).file(new MockMultipartFile("receiptFile", "receipt.pdf", "application/pdf", new byte[]{1}));
        if (update) builder.with(request -> { request.setMethod("PUT"); return request; });
        return token(builder.param("amount", "100.00").param("paymentMethod", "PIX").param("paidAt", NOW.toString()));
    }
    @Test void paymentRejectionResubmissionConfirmationAndRefundProtectFinancialData() throws Exception {
        var debt = seedCharge(); auth(10L); when(storage.store(any(), any())).thenReturn(new StoredFile("billing/payment-receipts/private-key.pdf"));
        long payment = id(mvc.perform(upload("/billing/member-charges/{id}/payments", debt.getId(), false)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptStorageKey").doesNotExist()).andExpect(jsonPath("$.receiptUrl").exists()).andReturn());
        auth(99L, "BILLING_PAYMENT_REJECT");
        mvc.perform(body(patch("/billing/member-payments/{id}/reject", payment), "{\"rejectionReason\":\"unreadable\"}")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        auth(10L); mvc.perform(upload("/billing/member-payments/{id}", payment, true)).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING_VALIDATION"));
        mvc.perform(token(post("/billing/member-payments/{id}/refund-request", payment))).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REFUND_PAYMENT"));
        auth(99L, "BILLING_PAYMENT_CONFIRM", "BILLING_PAYMENT_READ", "BILLING_MEMBER_CHARGE_READ");
        mvc.perform(token(patch("/billing/member-payments/{id}/confirm", payment))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        mvc.perform(token(get("/billing/member-payments/{id}", payment))).andExpect(status().isOk()).andExpect(jsonPath("$.receiptStorageKey").doesNotExist());
        mvc.perform(token(get("/billing/member-charges/{id}", debt.getId()))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
        mvc.perform(token(get("/billing/member-payments"))).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].receiptStorageKey").doesNotExist()).andExpect(jsonPath("$.pageable").doesNotExist());
        auth(10L); long refund = id(mvc.perform(token(post("/billing/member-payments/{id}/refund-request", payment))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REQUESTED")).andReturn());
        mvc.perform(token(post("/billing/member-payments/{id}/refund-request", payment))).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(refund));
        mvc.perform(token(get("/billing/users/me/member-refunds"))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        auth(99L, "BILLING_REFUND_APPROVE", "BILLING_REFUND_MARK_AS_REFUNDED", "BILLING_REFUND_READ");
        mvc.perform(token(patch("/billing/member-refunds/{id}/approve", refund))).andExpect(status().isOk());
        mvc.perform(token(patch("/billing/member-refunds/{id}/mark-as-refunded", refund))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REFUNDED"));
        mvc.perform(token(get("/billing/member-refunds/{id}", refund))).andExpect(status().isOk());
        mvc.perform(token(get("/billing/member-refunds"))).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(refund));
        mvc.perform(token(get("/billing/charge-cycles/{id}/member-refunds", debt.getChargeCycleId()))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        assertThat(payments.findById(payment).orElseThrow().getReceiptStorageKey()).isEqualTo("billing/payment-receipts/private-key.pdf");
    }
    @Test void memberOwnershipAndPendingPaymentConflictUseRfc9457WithoutInternals() throws Exception {
        var debt = seedCharge(); auth(20L);
        mvc.perform(token(get("/billing/me/member-charges/{id}", debt.getId()))).andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json")).andExpect(jsonPath("$.code").value("MEMBER_CHARGE_ACCESS_DENIED"))
                .andExpect(jsonPath("$.timestamp").exists());
        mvc.perform(token(get("/billing/me/member-charges"))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        auth(10L); mvc.perform(token(get("/billing/me/member-charges?status=PENDING"))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(token(get("/billing/me/member-charges/{id}", debt.getId()))).andExpect(status().isOk());
        when(storage.store(any(), any())).thenReturn(new StoredFile("private-key"));
        mvc.perform(upload("/billing/member-charges/{id}/payments", debt.getId(), false)).andExpect(status().isCreated());
        mvc.perform(upload("/billing/member-charges/{id}/payments", debt.getId(), false)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEMBER_PAYMENT_ALREADY_EXISTS"));
        auth(99L, "BILLING_MEMBER_CHARGE_UPDATE");
        mvc.perform(body(patch("/billing/member-charges/{id}/final-amount", debt.getId()), "{\"finalAmount\":50}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MEMBER_CHARGE_CANNOT_UPDATE_FINAL_AMOUNT"));
    }
    @ParameterizedTest @ValueSource(strings = {"{}", "{\"name\":\" \",\"defaultAmount\":0}", "{\"name\":\"fee\",\"recurrenceType\":\"INVALID\"}"})
    void invalidDefinitionPayloadUsesBeanValidationProblem(String payload) throws Exception {
        auth(99L, "BILLING_CHARGE_DEFINITION_CREATE");
        mvc.perform(body(post("/billing/charge-definitions"), payload)).andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }
    @Test void missingAndInvalidIdsAndMultipartUseControlledProblems() throws Exception {
        auth(99L, "BILLING_CHARGE_DEFINITION_READ", "BILLING_PAYMENT_READ", "BILLING_REFUND_READ", "BILLING_CHARGE_ASSIGNMENT_READ", "BILLING_CHARGE_CYCLE_READ");
        for (var path : List.of("/billing/charge-definitions/999999", "/billing/member-payments/999999", "/billing/member-refunds/999999", "/billing/charge-assignments/999999", "/billing/charge-cycles/999999")) {
            mvc.perform(token(get(path))).andExpect(status().isNotFound()).andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        }
        mvc.perform(token(get("/billing/charge-definitions/0"))).andExpect(status().isBadRequest());
        auth(10L);
        mvc.perform(token(multipart("/billing/member-charges/1/payments").param("amount", "0")))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }
    @Test void authorityIsSpecificToFinancialActionAndUnauthenticatedRequestsAreRejected() throws Exception {
        var routes = List.of(get("/billing/member-charges"), get("/billing/member-payments"), get("/billing/member-refunds"),
                get("/billing/charge-definitions"), patch("/billing/member-payments/1/confirm"), patch("/billing/member-refunds/1/approve"),
                patch("/billing/member-refunds/1/expire"), patch("/billing/charge-cycles/1/cancel"), patch("/billing/member-charges/1/cancel"));
        for (var route : routes) mvc.perform(route).andExpect(status().isUnauthorized());
        auth(99L, "BILLING_EXPORT");
        for (var route : routes) mvc.perform(token(route)).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }
    @Test void allAssignmentHttpKindsAndLifecyclePreserveTheirTargets() throws Exception {
        auth(99L, "BILLING_CHARGE_DEFINITION_CREATE", "BILLING_CHARGE_DEFINITION_READ", "BILLING_CHARGE_ASSIGNMENT_CREATE", "BILLING_CHARGE_ASSIGNMENT_UPDATE", "BILLING_CHARGE_ASSIGNMENT_READ");
        long definition = id(mvc.perform(body(post("/billing/charge-definitions"), DEFINITION)).andExpect(status().isCreated()).andReturn());
        mvc.perform(token(get("/billing/charge-definitions/{id}", definition))).andExpect(status().isOk());
        mvc.perform(token(get("/billing/charge-definitions"))).andExpect(status().isOk()).andExpect(jsonPath("$.content[0].name").value("fee"));
        when(roles.existsActiveRoleById(20L)).thenReturn(true); when(events.existsEventById(30L)).thenReturn(true);
        for (String suffix : List.of("all-members", "roles/20", "events/30/participants")) {
            long assignment = id(mvc.perform(token(post("/billing/charge-definitions/{id}/assignments/" + suffix, definition))).andExpect(status().isCreated()).andReturn());
            mvc.perform(token(get("/billing/charge-assignments/{id}", assignment))).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));
            mvc.perform(token(patch("/billing/charge-assignments/{id}/deactivate", assignment))).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(false));
            mvc.perform(token(patch("/billing/charge-assignments/{id}/activate", assignment))).andExpect(status().isOk()).andExpect(jsonPath("$.active").value(true));
            mvc.perform(token(patch("/billing/charge-assignments/{id}/activate", assignment))).andExpect(status().isConflict()).andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        }
    }
    @Test void administrativeChargeAmountAndCancellationAreReflectedInMemberReads() throws Exception {
        var debt = seedCharge(); auth(99L, "BILLING_MEMBER_CHARGE_UPDATE", "BILLING_MEMBER_CHARGE_CANCEL", "BILLING_MEMBER_CHARGE_READ");
        mvc.perform(body(patch("/billing/member-charges/{id}/final-amount", debt.getId()), "{\"finalAmount\":50}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.finalAmount").value(50));
        mvc.perform(token(get("/billing/member-charges?userId=10&status=PENDING"))).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(token(patch("/billing/member-charges/{id}/cancel", debt.getId()))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELED"));
        auth(10L); mvc.perform(token(get("/billing/me/member-charges/{id}", debt.getId()))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELED"));
    }
    @Test void refundRequestsAndAdministrativeRejectCancelExpireFollowSpecificPermissionsAndStates() throws Exception {
        var request = refunds.save(com.jeepclub.backend.billing.support.BillingFixtures.request());
        auth(99L, "BILLING_REFUND_REJECT", "BILLING_REFUND_CANCEL", "BILLING_REFUND_EXPIRE");
        mvc.perform(body(patch("/billing/member-refunds/{id}/reject", request.getId()), "{\"rejectionReason\":\" \"}")).andExpect(status().isBadRequest());
        mvc.perform(body(patch("/billing/member-refunds/{id}/reject", request.getId()), "{\"rejectionReason\":\"not refundable\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REJECTED"));
        var canceled = refunds.save(com.jeepclub.backend.billing.support.BillingFixtures.request());
        mvc.perform(token(patch("/billing/member-refunds/{id}/cancel", canceled.getId()))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELED"));
        var expired = refunds.save(MemberRefund.createEligibilityForCanceledCycle(2L, 1L, 4L, 10L, AMOUNT, 99L, NOW.minusSeconds(100), NOW, NOW));
        mvc.perform(token(patch("/billing/member-refunds/{id}/expire", expired.getId()))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EXPIRED"));
        var eligible = refunds.save(eligibility()); auth(20L);
        mvc.perform(token(patch("/billing/member-refunds/{id}/request", eligible.getId()))).andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("MEMBER_REFUND_ACCESS_DENIED"));
        auth(10L); mvc.perform(token(patch("/billing/member-refunds/{id}/request", eligible.getId()))).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("REQUESTED"));
    }
}
