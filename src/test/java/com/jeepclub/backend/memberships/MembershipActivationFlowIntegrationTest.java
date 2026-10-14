package com.jeepclub.backend.memberships;

import com.jeepclub.backend.memberships.core.application.service.memberactivationtoken.MemberActivationTokenService;
import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.memberships.core.port.ActivationRecipient;
import com.jeepclub.backend.memberships.core.port.ActivationRecipientPort;
import com.jeepclub.backend.memberships.core.port.CompletePendingFirstAccessPort;
import com.jeepclub.backend.memberships.core.port.MemberActivationMailSender;
import com.jeepclub.backend.memberships.core.repository.MembershipApplicationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MembershipActivationFlowIntegrationTest {

    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");
    private static final Long CREATED_USER_ID = 88L;
    private static final String CANDIDATE_EMAIL = "candidate@jeepclub.com";
    private static final String CANDIDATE_NAME = "Candidate Jeep Club";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MembershipApplicationRepository applicationRepository;

    @Autowired
    private MemberActivationTokenService activationTokenService;

    @MockitoBean
    private CompletePendingFirstAccessPort completePendingFirstAccessPort;

    @MockitoBean
    private ActivationRecipientPort activationRecipientPort;

    @MockitoBean
    private MemberActivationMailSender mailSender;

    @Test
    @DisplayName("Complete deterministic onboarding flow: Issue -> Validate -> Complete first access -> Idempotency checks")
    void completeDeterministicOnboardingFlow() throws Exception {
        // 1. Arrange: Persist an approved membership application awaiting activation
        MembershipApplication application = MembershipApplication.create(
                CANDIDATE_NAME,
                "52998224725",
                CANDIDATE_EMAIL,
                "11988887777",
                "Quero participar",
                NOW
        );
        application.approve(10L, CREATED_USER_ID, NOW);
        MembershipApplication savedApplication = applicationRepository.save(application);

        when(activationRecipientPort.findByIdentityId(CREATED_USER_ID))
                .thenReturn(Optional.of(new ActivationRecipient(CANDIDATE_NAME, CANDIDATE_EMAIL)));

        // 2. Admin issues activation token and invitation email
        activationTokenService.issueAndSend(savedApplication);

        ArgumentCaptor<String> rawTokenCaptor = ArgumentCaptor.forClass(String.class);
        verify(mailSender).sendActivationLink(
                eq(CANDIDATE_EMAIL),
                eq(CANDIDATE_NAME),
                rawTokenCaptor.capture()
        );
        String rawToken = rawTokenCaptor.getValue();
        assertThat(rawToken).isNotBlank();

        // 3. User clicks link received by email: GET /membership-applications/activate?token=...
        // Proves that validation is read-only and succeeds with 204 No Content
        mockMvc.perform(get("/membership-applications/activate")
                        .param("token", rawToken))
                .andExpect(status().isNoContent());

        // Verify application status is still APPROVED and not prematurely COMPLETED
        MembershipApplication appAfterValidation = applicationRepository.findById(savedApplication.getId()).orElseThrow();
        assertThat(appAfterValidation.getStatus()).isEqualTo(MembershipApplicationStatus.APPROVED);
        assertThat(appAfterValidation.getFinishedAt()).isNull();

        // 4. User defines password: POST /membership-applications/activate
        String completePayload = """
                {
                  "token": "%s",
                  "newPassword": "DefinitivePassword2026!"
                }
                """.formatted(rawToken);

        mockMvc.perform(post("/membership-applications/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completePayload))
                .andExpect(status().isNoContent());

        // 5. Verify side effects: Identity first access completed and Application status updated to COMPLETED
        verify(completePendingFirstAccessPort).complete(CREATED_USER_ID, "DefinitivePassword2026!");

        MembershipApplication appAfterCompletion = applicationRepository.findById(savedApplication.getId()).orElseThrow();
        assertThat(appAfterCompletion.getStatus()).isEqualTo(MembershipApplicationStatus.COMPLETED);
        assertThat(appAfterCompletion.getFinishedAt()).isNotNull();

        // 6. Security & Idempotency: Re-submitting the token for validation or completion fails with 409 Conflict
        mockMvc.perform(get("/membership-applications/activate")
                        .param("token", rawToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTIVATION_TOKEN_ALREADY_USED"));

        mockMvc.perform(post("/membership-applications/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completePayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTIVATION_TOKEN_ALREADY_USED"));
    }
}
