package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.iam.authentication.api.http.controller.admin.AdminPasswordRecoveryRequestController;
import com.jeepclub.backend.iam.authentication.core.application.result.PasswordResetLinkAdminResult;
import com.jeepclub.backend.iam.authentication.core.application.result.TemporaryPasswordAdminResult;
import com.jeepclub.backend.iam.authentication.core.application.service.passwordrecovery.AdminPasswordRecoveryService;
import com.jeepclub.backend.iam.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.iam.authentication.core.domain.enums.PasswordRecoveryRequestStatus;
import com.jeepclub.backend.iam.authentication.core.domain.model.PasswordRecoveryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminPasswordRecoveryControllerTest {

    @Mock
    private AdminPasswordRecoveryService adminPasswordRecoveryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        AdminPasswordRecoveryRequestController controller =
                new AdminPasswordRecoveryRequestController(adminPasswordRecoveryService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("Sucesso: administrador gera senha provisÃ³ria")
    void shouldReturnOkWhenAdminGeneratesTemporaryPassword() throws Exception {
        Instant createdAt = Instant.parse("2026-05-21T18:00:00Z");
        Instant expiresAt = Instant.parse("2026-05-28T18:00:00Z");

        PasswordRecoveryRequest recoveryRequest =
                PasswordRecoveryRequest.createOpenRequest(
                        1L,
                        createdAt,
                        expiresAt
                );

        recoveryRequest.changeToAdminTemporaryPasswordMethod(createdAt);

        TemporaryPasswordAdminResult result =
                new TemporaryPasswordAdminResult(
                        "SenhaTemp@123",
                        recoveryRequest
                );

        when(adminPasswordRecoveryService.generateTemporaryPassword(anyLong()))
                .thenReturn(result);

        mockMvc.perform(post("/authentication/admin/password-recovery/requests/users/{userId}/temporary-password", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporaryPassword").value("SenhaTemp@123"))
                .andExpect(jsonPath("$.status").value(PasswordRecoveryRequestStatus.OPEN.name()))
                .andExpect(jsonPath("$.method").value(PasswordRecoveryRequestMethod.ADMIN_TEMPORARY_PASSWORD.name()));
    }

    @Test
    @DisplayName("Sucesso: administrador gera link de redefiniÃ§Ã£o")
    void shouldReturnOkWhenAdminGeneratesResetLink() throws Exception {
        Instant createdAt = Instant.parse("2026-05-21T18:00:00Z");
        Instant expiresAt = Instant.parse("2026-05-28T18:00:00Z");

        PasswordRecoveryRequest recoveryRequest =
                PasswordRecoveryRequest.createOpenRequest(
                        1L,
                        createdAt,
                        expiresAt
                );

        recoveryRequest.changeToAdminResetLinkMethod(
                "hashed-token-example",
                createdAt
        );

        PasswordResetLinkAdminResult result =
                new PasswordResetLinkAdminResult(
                        "https://jeepclub.com.br/password-recovery/reset?token=tokenAdmin123",
                        recoveryRequest
                );

        when(adminPasswordRecoveryService.generateResetLink(anyLong()))
                .thenReturn(result);

        mockMvc.perform(post("/authentication/admin/password-recovery/requests/users/{userId}/reset-link", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetLink").value("https://jeepclub.com.br/password-recovery/reset?token=tokenAdmin123"))
                .andExpect(jsonPath("$.request.status").value(PasswordRecoveryRequestStatus.OPEN.name()))
                .andExpect(jsonPath("$.request.method").value(PasswordRecoveryRequestMethod.ADMIN_RESET_LINK.name()));
    }
}
