package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.application.services.LoginService;
import com.jeepclub.backend.infra.security.jwt.JwtTokenParser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoginController.class)
@AutoConfigureMockMvc(addFilters = false)
@AutoConfigureRestDocs(outputDir = "target/generated-snippets")
class LoginControllerRestDocsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginService loginService;

    @MockitoBean
    private JwtTokenParser jwtTokenParser;

    @Test
    void shouldDocumentLoginSuccess() throws Exception {
        AuthTokens tokens = new AuthTokens(
                "refresh-token-example",
                "access-token-example",
                3600L
        );

        when(loginService.login("52998224725", "senha123"))
                .thenReturn(tokens);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "cpf": "52998224725",
                                  "senha": "senha123"
                                }
                                """))
                .andExpect(status().isOk())
                .andDo(document("auth-login-success",
                        requestFields(
                                fieldWithPath("cpf").description("CPF válido do usuário utilizado para autenticação"),
                                fieldWithPath("senha").description("Senha do usuário")
                        ),
                        responseFields(
                                fieldWithPath("refreshToken").description("Token utilizado para renovar a autenticação"),
                                fieldWithPath("accessToken").description("Token JWT utilizado para acessar recursos protegidos"),
                                fieldWithPath("expiresInSeconds").description("Tempo de expiração do access token em segundos")
                        )
                ));
    }
}