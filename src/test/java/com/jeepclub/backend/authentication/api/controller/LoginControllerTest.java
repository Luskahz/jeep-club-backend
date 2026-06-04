package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.core.application.exceptions.user.UserCpfNotFoundException;
import com.jeepclub.backend.authentication.core.application.exceptions.user.UserInvalidPasswordException;
import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.application.results.login.AuthenticatedLoginResult;
import com.jeepclub.backend.authentication.core.application.results.login.PasswordChangeRequiredLoginResult;
import com.jeepclub.backend.authentication.core.application.services.AccessTokenAuthenticationService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SessionController.class)
@AutoConfigureMockMvc(addFilters = false)
class LoginControllerTest {

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
    @DisplayName("Sucesso: login com credenciais definitivas retorna 200 e tokens")
    void shouldReturnTokensOnSuccessfulLogin() throws Exception {
        AuthTokens tokens = new AuthTokens(
                "refresh-xyz",
                "access-xyz",
                3600L
        );

        when(loginService.login("52998224725", "senha123"))
                .thenReturn(new AuthenticatedLoginResult(tokens));

        String payload = """
                {
                    "cpf": "52998224725",
                    "senha": "senha123"
                }
                """;

        mockMvc.perform(post("/authentication/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHENTICATED"))
                .andExpect(jsonPath("$.accessToken").value("access-xyz"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-xyz"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600));
    }

    @Test
    @DisplayName("Sucesso: login com senha provisória retorna desafio de troca de senha")
    void shouldReturnPasswordChangeRequiredWhenTemporaryPasswordIsUsed() throws Exception {
        Instant expiresAt = Instant.parse("2026-05-21T20:30:00Z");

        when(loginService.login("52998224725", "senhaProvisoria123"))
                .thenReturn(new PasswordChangeRequiredLoginResult(
                        "password-change-token-xyz",
                        expiresAt
                ));

        String payload = """
                {
                    "cpf": "52998224725",
                    "senha": "senhaProvisoria123"
                }
                """;

        mockMvc.perform(post("/authentication/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PASSWORD_CHANGE_REQUIRED"))
                .andExpect(jsonPath("$.passwordChangeToken").value("password-change-token-xyz"))
                .andExpect(jsonPath("$.passwordChangeTokenExpiresAt").value("2026-05-21T20:30:00Z"));
    }

    @Test
    @DisplayName("Falha: senha incorreta retorna 401 Unauthorized")
    void shouldReturnUnauthorizedOnInvalidPassword() throws Exception {
        when(loginService.login(anyString(), anyString()))
                .thenThrow(new UserInvalidPasswordException(1L));

        String payload = """
                {
                    "cpf": "52998224725",
                    "senha": "senhaErrada"
                }
                """;

        mockMvc.perform(post("/authentication/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Falha: usuário inexistente retorna 404 Not Found")
    void shouldReturnNotFoundOnUserNotFound() throws Exception {
        when(loginService.login(anyString(), anyString()))
                .thenThrow(new UserCpfNotFoundException("CPF não encontrado"));

        String payload = """
                {
                    "cpf": "52998224725",
                    "senha": "senha123"
                }
                """;

        mockMvc.perform(post("/authentication/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Falha: dados inválidos retorna 400 Bad Request")
    void shouldReturnBadRequestOnInvalidData() throws Exception {
        String payload = """
                {
                    "cpf": "",
                    "senha": ""
                }
                """;

        mockMvc.perform(post("/authentication/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());
    }
}