package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.application.results.login.AuthenticatedLoginResult;
import com.jeepclub.backend.authentication.core.application.results.login.PasswordChangeRequiredLoginResult;
import com.jeepclub.backend.authentication.core.application.services.AccessTokenAuthenticationService;
import com.jeepclub.backend.authentication.core.application.services.LoginService;
import com.jeepclub.backend.platform.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.platform.security.jwt.JwtTokenParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessionController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
class LoginControllerRestDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginService loginService;

    @MockitoBean
    private JwtTokenParser jwtTokenParser;

    @MockitoBean
    private UserAuthoritiesProvider userAuthoritiesProvider;

    @MockitoBean
    private AccessTokenAuthenticationService accessTokenAuthenticationService;

    @Test
    void shouldDocumentLoginAuthenticated() throws Exception {
        AuthTokens tokens = new AuthTokens(
                "refresh-token-example",
                "access-token-example",
                3600L
        );

        when(loginService.login("52998224725", "senha123"))
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
                                fieldWithPath("cpf").description("CPF válido do usuário utilizado para autenticação"),
                                fieldWithPath("senha").description("Senha do usuário")
                        ),
                        responseFields(
                                fieldWithPath("status").description("Status do fluxo de login. Neste caso, AUTHENTICATED"),
                                fieldWithPath("refreshToken").description("Token utilizado para renovar a autenticação"),
                                fieldWithPath("accessToken").description("Token JWT utilizado para acessar recursos protegidos"),
                                fieldWithPath("expiresInSeconds").description("Tempo de expiração do access token em segundos")
                        )
                ));
    }

    @Test
    void shouldDocumentLoginPasswordChangeRequired() throws Exception {
        Instant expiresAt = Instant.parse("2026-05-21T20:30:00Z");

        when(loginService.login("52998224725", "senhaProvisoria123"))
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
                                fieldWithPath("cpf").description("CPF válido do usuário utilizado para autenticação"),
                                fieldWithPath("senha").description("Senha provisória do usuário")
                        ),
                        responseFields(
                                fieldWithPath("status").description("Status do fluxo de login. Neste caso, PASSWORD_CHANGE_REQUIRED"),
                                fieldWithPath("passwordChangeToken").description("Token temporário usado para concluir a troca obrigatória de senha"),
                                fieldWithPath("passwordChangeTokenExpiresAt").description("Data e hora de expiração do token temporário de troca de senha")
                        )
                ));
    }
}