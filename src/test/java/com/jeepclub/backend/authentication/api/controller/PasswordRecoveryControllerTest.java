package com.jeepclub.backend.authentication.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jeepclub.backend.authentication.api.http.controller.PasswordRecoveryRequestController;
import com.jeepclub.backend.authentication.api.http.exception.PasswordRecoveryExceptionHandler;
import com.jeepclub.backend.authentication.core.application.result.PublicPasswordRecoveryResult;
import com.jeepclub.backend.authentication.core.application.service.passwordrecovery.PasswordRecoveryService;
import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestStatus;
import com.jeepclub.backend.authentication.core.domain.model.PasswordRecoveryRequest;
import com.jeepclub.backend.platform.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryControllerTest {

    private static final String BASE_PATH =
            "/authentication/password-recovery/requests";

    private static final String CPF = "52998224725";

    private static final Instant CREATED_AT =
            Instant.parse("2026-05-21T18:00:00Z");

    private static final Instant EXPIRES_AT =
            Instant.parse("2026-05-28T18:00:00Z");

    @Mock
    private PasswordRecoveryService passwordRecoveryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        PasswordRecoveryRequestController controller =
                new PasswordRecoveryRequestController(passwordRecoveryService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(
                        new GlobalExceptionHandler(),
                        new PasswordRecoveryExceptionHandler()
                )
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("Sucesso: cria ou consulta solicitação aberta de recuperação")
    void shouldCreateOrGetOpenRecoveryRequest() throws Exception {
        PasswordRecoveryRequest recoveryRequest =
                createOpenRecoveryRequest();

        when(
                passwordRecoveryService.request(CPF)
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

        verify(passwordRecoveryService).request(CPF);
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
                passwordRecoveryService.sendEmailToken(CPF)
        ).thenReturn(PublicPasswordRecoveryResult.from(recoveryRequest));

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

        verify(passwordRecoveryService).sendEmailToken(CPF);
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

        verify(passwordRecoveryService).resetPasswordByToken(token, newPassword);
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
