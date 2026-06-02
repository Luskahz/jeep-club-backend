package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.api.controller.passwordRecovery.AdminPasswordRecoveryRequestController;
import com.jeepclub.backend.authentication.core.application.results.PasswordResetLinkAdminResult;
import com.jeepclub.backend.authentication.core.application.results.TemporaryPasswordAdminResult;
import com.jeepclub.backend.authentication.core.application.services.AccessTokenAuthenticationService;
import com.jeepclub.backend.authentication.core.application.services.PasswordRecoveryService;
import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestStatus;
import com.jeepclub.backend.authentication.core.domain.model.PasswordRecoveryRequest;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminPasswordRecoveryRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminPasswordRecoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasswordRecoveryService passwordRecoveryService;

    @MockitoBean
    private JwtTokenParser jwtTokenParser;

    @MockitoBean
    private UserAuthoritiesProvider userAuthoritiesProvider;

    @MockitoBean
    private AccessTokenAuthenticationService accessTokenAuthenticationService;

    @Test
    @DisplayName("Sucesso: administrador gera senha provisória")
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

        when(passwordRecoveryService.generateTemporaryPasswordByAdmin(anyLong()))
                .thenReturn(result);

        mockMvc.perform(post("/authentication/admin/password-recovery/requests/users/{userId}/temporary-password", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporaryPassword").value("SenhaTemp@123"))
                .andExpect(jsonPath("$.request.status").value(PasswordRecoveryRequestStatus.OPEN.name()))
                .andExpect(jsonPath("$.request.method").value(PasswordRecoveryRequestMethod.ADMIN_TEMPORARY_PASSWORD.name()));
    }

    @Test
    @DisplayName("Sucesso: administrador gera link de redefinição")
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

        when(passwordRecoveryService.generateResetLinkByAdmin(anyLong()))
                .thenReturn(result);

        mockMvc.perform(post("/authentication/admin/password-recovery/requests/users/{userId}/reset-link", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetLink").value("https://jeepclub.com.br/password-recovery/reset?token=tokenAdmin123"))
                .andExpect(jsonPath("$.request.status").value(PasswordRecoveryRequestStatus.OPEN.name()))
                .andExpect(jsonPath("$.request.method").value(PasswordRecoveryRequestMethod.ADMIN_RESET_LINK.name()));
    }
}