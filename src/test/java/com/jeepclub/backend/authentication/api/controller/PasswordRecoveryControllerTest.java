package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.api.controller.passwordRecovery.PasswordRecoveryRequestController;
import com.jeepclub.backend.authentication.core.application.results.PublicPasswordRecoveryResult;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordRecoveryRequestController.class)
@AutoConfigureMockMvc(addFilters = false)
class PasswordRecoveryControllerTest {

    private static final String BASE_PATH =
            "/authentication/password-recovery/requests";

    private static final String CPF = "52998224725";

    private static final Instant CREATED_AT =
            Instant.parse("2026-05-21T18:00:00Z");

    private static final Instant EXPIRES_AT =
            Instant.parse("2026-05-28T18:00:00Z");

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
        PasswordRecoveryRequest recoveryRequest =
                createOpenRecoveryRequest();

        when(
                passwordRecoveryService
                        .createOrGetOpenRecoveryRequest(CPF)
        ).thenReturn(PublicPasswordRecoveryResult.from(recoveryRequest));

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cpfPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(
                        PasswordRecoveryRequestStatus.OPEN.name()
                ))
                .andExpect(jsonPath("$.method").value(
                        PasswordRecoveryRequestMethod.UNDEFINED.name()
                ))
                .andExpect(jsonPath("$.createdAt").value(
                        CREATED_AT.toString()
                ))
                .andExpect(jsonPath("$.expiresAt").value(
                        EXPIRES_AT.toString()
                ))
                .andExpect(jsonPath("$.resolvedAt").doesNotExist())
                .andExpect(jsonPath("$.cancelledAt").doesNotExist());

        verify(passwordRecoveryService)
                .createOrGetOpenRecoveryRequest(CPF);
    }

    @Test
    @DisplayName("Sucesso: envia token de recuperação por e-mail")
    void shouldSendRecoveryEmailToken() throws Exception {
        PasswordRecoveryRequest recoveryRequest =
                createOpenRecoveryRequest();

        recoveryRequest.changeToEmailTokenMethod(
                "hashed-token-example",
                CREATED_AT
        );

        when(
                passwordRecoveryService
                        .sendRecoveryEmailToken(CPF)
        ).thenReturn(recoveryRequest);

        mockMvc.perform(post(BASE_PATH + "/email-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cpfPayload()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(
                        PasswordRecoveryRequestStatus.OPEN.name()
                ))
                .andExpect(jsonPath("$.method").value(
                        PasswordRecoveryRequestMethod.EMAIL_TOKEN.name()
                ))
                .andExpect(jsonPath("$.createdAt").value(
                        CREATED_AT.toString()
                ))
                .andExpect(jsonPath("$.expiresAt").value(
                        EXPIRES_AT.toString()
                ))
                .andExpect(jsonPath("$.resolvedAt").doesNotExist())
                .andExpect(jsonPath("$.cancelledAt").doesNotExist());

        verify(passwordRecoveryService)
                .sendRecoveryEmailToken(CPF);
    }

    @Test
    @DisplayName("Sucesso: redefine senha por token e retorna 204")
    void shouldReturnNoContentWhenResetPasswordByToken() throws Exception {
        String token = "tokenMagico";
        String newPassword = "NovaSenhaSuperForte@123";

        String payload = """
                {
                    "token": "%s",
                    "newPassword": "%s"
                }
                """.formatted(token, newPassword);

        mockMvc.perform(post(BASE_PATH + "/token/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNoContent());

        verify(passwordRecoveryService)
                .resetPasswordByToken(token, newPassword);
    }

    private PasswordRecoveryRequest createOpenRecoveryRequest() {
        return PasswordRecoveryRequest.createOpenRequest(
                1L,
                CREATED_AT,
                EXPIRES_AT
        );
    }

    private String cpfPayload() {
        return """
                {
                    "cpf": "%s"
                }
                """.formatted(CPF);
    }
}