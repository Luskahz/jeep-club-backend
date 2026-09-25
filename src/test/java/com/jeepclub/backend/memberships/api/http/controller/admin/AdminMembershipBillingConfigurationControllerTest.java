package com.jeepclub.backend.memberships.api.http.controller.admin;

import com.jeepclub.backend.memberships.api.http.exception.MembershipExceptionHandler;
import com.jeepclub.backend.memberships.core.application.exception.InvalidMembershipChargeDefinitionException;
import com.jeepclub.backend.memberships.core.application.exception.MembershipBillingConfigurationNotFoundException;
import com.jeepclub.backend.memberships.core.application.service.membershipbilling.AdminMembershipBillingConfigurationService;
import com.jeepclub.backend.memberships.core.domain.model.MembershipBillingConfiguration;
import com.jeepclub.backend.platform.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AdminMembershipBillingConfigurationControllerTest {

    @Mock
    private AdminMembershipBillingConfigurationService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JsonMapper jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new AdminMembershipBillingConfigurationController(service)
                )
                .setControllerAdvice(new MembershipExceptionHandler(), new GlobalExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper))
                .build();
    }

    @Test
    void shouldReturnNoContentWhenConfigurationIsMissing() throws Exception {
        when(service.getCurrent()).thenReturn(Optional.empty());

        mockMvc.perform(get("/admin/membership-billing-configuration"))
                .andExpect(status().isNoContent());
    }

    @Test
    void shouldConfigureAndChangeEnforcement() throws Exception {
        MembershipBillingConfiguration configuration = configuration(true);
        when(service.configure(12L, true)).thenReturn(configuration);
        when(service.setEnforcementEnabled(false)).thenReturn(configuration(false));

        mockMvc.perform(put("/admin/membership-billing-configuration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chargeDefinitionId\":12,\"enforcementEnabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chargeDefinitionId").value(12))
                .andExpect(jsonPath("$.enforcementEnabled").value(true));

        mockMvc.perform(patch("/admin/membership-billing-configuration/enforcement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enforcementEnabled\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chargeDefinitionId").value(12))
                .andExpect(jsonPath("$.enforcementEnabled").value(false));
    }

    @Test
    void shouldRejectInvalidPayloadAndDefinitionWithProblems() throws Exception {
        mockMvc.perform(put("/admin/membership-billing-configuration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chargeDefinitionId\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        when(service.configure(99L, true)).thenThrow(new InvalidMembershipChargeDefinitionException());
        mockMvc.perform(put("/admin/membership-billing-configuration")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"chargeDefinitionId\":99,\"enforcementEnabled\":true}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("MEMBERSHIP_CHARGE_DEFINITION_INVALID"));
    }

    @Test
    void shouldReturnNotFoundWhenChangingEnforcementWithoutConfiguration() throws Exception {
        when(service.setEnforcementEnabled(true))
                .thenThrow(new MembershipBillingConfigurationNotFoundException());

        mockMvc.perform(patch("/admin/membership-billing-configuration/enforcement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enforcementEnabled\":true}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBERSHIP_BILLING_CONFIGURATION_NOT_FOUND"));
    }

    private static MembershipBillingConfiguration configuration(boolean enabled) {
        Instant now = Instant.parse("2026-09-21T12:00:00Z");
        return MembershipBillingConfiguration.reconstitute(1L, 12L, enabled, now, now);
    }
}
