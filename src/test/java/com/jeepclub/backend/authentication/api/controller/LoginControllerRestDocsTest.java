package com.jeepclub.backend.authentication.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jeepclub.backend.authentication.api.http.controller.SessionController;
import com.jeepclub.backend.authentication.api.http.exception.PasswordChangeChallengeExceptionHandler;
import com.jeepclub.backend.authentication.api.http.exception.SessionExceptionHandler;
import com.jeepclub.backend.authentication.api.http.exception.AuthenticationAccountExceptionHandler;
import com.jeepclub.backend.authentication.core.application.result.AuthTokens;
import com.jeepclub.backend.authentication.core.application.result.login.AuthenticatedLoginResult;
import com.jeepclub.backend.authentication.core.application.result.login.PasswordChangeRequiredLoginResult;
import com.jeepclub.backend.authentication.core.application.service.session.SessionService;
import com.jeepclub.backend.platform.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith({MockitoExtension.class, RestDocumentationExtension.class})
class LoginControllerRestDocsTest {

    @Mock
    private SessionService sessionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp(RestDocumentationContextProvider restDocumentation) {
        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        SessionController controller = new SessionController(sessionService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(
                        new GlobalExceptionHandler(),
                        new AuthenticationAccountExceptionHandler(),
                        new SessionExceptionHandler(),
                        new PasswordChangeChallengeExceptionHandler()
                )
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .apply(documentationConfiguration(restDocumentation))
                .build();
    }

    @Test
    void shouldDocumentLoginAuthenticated() throws Exception {
        AuthTokens tokens = new AuthTokens(
                "refresh-token-example",
                "access-token-example",
                3600L
        );

        when(sessionService.login("52998224725", "senha123"))
                .thenReturn(new AuthenticatedLoginResult(tokens));

        mockMvc.perform(post("/authentication/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cpf": "52998224725",
                                  "senha": "senha123"
                                }
                                """))
                .andExpect(status().isOk())
                .andDo(document("authentication-login-authenticated",
                        requestFields(
                                fieldWithPath("cpf").description("CPF valido do usuario utilizado para autenticacao"),
                                fieldWithPath("senha").description("Senha do usuario")
                        ),
                        responseFields(
                                fieldWithPath("status").description("Status do fluxo de login. Neste caso, AUTHENTICATED"),
                                fieldWithPath("refreshToken").description("Token utilizado para renovar a autenticacao"),
                                fieldWithPath("accessToken").description("Token JWT utilizado para acessar recursos protegidos"),
                                fieldWithPath("expiresInSeconds").description("Tempo de expiracao do access token em segundos"),
                                fieldWithPath("passwordChangeToken").type(JsonFieldType.STRING).optional().description("Nao retornado quando o login ja foi concluido."),
                                fieldWithPath("passwordChangeTokenExpiresAt").type(JsonFieldType.STRING).optional().description("Nao retornado quando o login ja foi concluido.")
                        )
                ));
    }

    @Test
    void shouldDocumentLoginPasswordChangeRequired() throws Exception {
        Instant expiresAt = Instant.parse("2026-05-21T20:30:00Z");

        when(sessionService.login("52998224725", "senhaProvisoria123"))
                .thenReturn(new PasswordChangeRequiredLoginResult(
                        "password-change-token-example",
                        expiresAt
                ));

        mockMvc.perform(post("/authentication/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cpf": "52998224725",
                                  "senha": "senhaProvisoria123"
                                }
                                """))
                .andExpect(status().isOk())
                .andDo(document("authentication-login-password-change-required",
                        requestFields(
                                fieldWithPath("cpf").description("CPF valido do usuario utilizado para autenticacao"),
                                fieldWithPath("senha").description("Senha provisoria do usuario")
                        ),
                        responseFields(
                                fieldWithPath("status").description("Status do fluxo de login. Neste caso, PASSWORD_CHANGE_REQUIRED"),
                                fieldWithPath("passwordChangeToken").description("Token temporario usado para concluir a troca obrigatoria de senha"),
                                fieldWithPath("passwordChangeTokenExpiresAt").description("Data e hora de expiracao do token temporario de troca de senha"),
                                fieldWithPath("refreshToken").type(JsonFieldType.STRING).optional().description("Nao retornado enquanto a troca obrigatoria de senha nao for concluida."),
                                fieldWithPath("accessToken").type(JsonFieldType.STRING).optional().description("Nao retornado enquanto a troca obrigatoria de senha nao for concluida."),
                                fieldWithPath("expiresInSeconds").type(JsonFieldType.NUMBER).optional().description("Nao retornado enquanto a troca obrigatoria de senha nao for concluida.")
                        )
                ));
    }
}
