package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.api.controller.passwordRecovery.PasswordRecoveryRequestController;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordRecoveryRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
class PasswordRecoveryControllerTest {

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
    @DisplayName("Sucesso: cria ou consulta solicitação aberta de recuperação")
    void shouldCreateOrGetOpenRecoveryRequest() throws Exception {
        Instant createdAt = Instant.parse("2026-05-21T18:00:00Z");
        Instant expiresAt = Instant.parse("2026-05-28T18:00:00Z");

        PasswordRecoveryRequest recoveryRequest =
                PasswordRecoveryRequest.createOpenRequest(
                        1L,
                        createdAt,
                        expiresAt
                );

        when(passwordRecoveryService.createOrGetOpenRecoveryRequest(anyString()))
                .thenReturn(recoveryRequest);

        String payload = """
                {
                    "cpf": "52998224725"
                }
                """;

        mockMvc.perform(post("/authentication/password-recovery/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(PasswordRecoveryRequestStatus.OPEN.name()))
                .andExpect(jsonPath("$.method").value(PasswordRecoveryRequestMethod.UNDEFINED.name()))
                .andExpect(jsonPath("$.createdAt").value("2026-05-21T18:00:00Z"))
                .andExpect(jsonPath("$.expiresAt").value("2026-05-28T18:00:00Z"))
                .andExpect(jsonPath("$.resolvedAt").doesNotExist())
                .andExpect(jsonPath("$.cancelledAt").doesNotExist());
    }

    @Test
    @DisplayName("Sucesso: envia token de recuperação por e-mail")
    void shouldSendRecoveryEmailToken() throws Exception {
        Instant createdAt = Instant.parse("2026-05-21T18:00:00Z");
        Instant expiresAt = Instant.parse("2026-05-28T18:00:00Z");

        PasswordRecoveryRequest recoveryRequest =
                PasswordRecoveryRequest.createOpenRequest(
                        1L,
                        createdAt,
                        expiresAt
                );

        recoveryRequest.changeToEmailTokenMethod(
                "hashed-token-example",
                createdAt
        );

        when(passwordRecoveryService.sendRecoveryEmailToken(anyString()))
                .thenReturn(recoveryRequest);

        String payload = """
                {
                    "cpf": "52998224725"
                }
                """;

        mockMvc.perform(post("/authentication/password-recovery/requests/email-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(PasswordRecoveryRequestStatus.OPEN.name()))
                .andExpect(jsonPath("$.method").value(PasswordRecoveryRequestMethod.EMAIL_TOKEN.name()))
                .andExpect(jsonPath("$.createdAt").value("2026-05-21T18:00:00Z"))
                .andExpect(jsonPath("$.expiresAt").value("2026-05-28T18:00:00Z"))
                .andExpect(jsonPath("$.resolvedAt").doesNotExist())
                .andExpect(jsonPath("$.cancelledAt").doesNotExist());
    }

    @Test
    @DisplayName("Sucesso: redefine senha por token e retorna 204")
    void shouldReturnNoContentWhenResetPasswordByToken() throws Exception {
        doNothing()
                .when(passwordRecoveryService)
                .resetPasswordByToken(anyString(), anyString());

        String payload = """
                {
                    "token": "tokenMagico",
                    "newPassword": "NovaSenhaSuperForte@123"
                }
                """;

        mockMvc.perform(post("/authentication/password-recovery/requests/token/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNoContent());
    }
}