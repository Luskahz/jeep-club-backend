package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.api.dto.login.LoginRequestDTO;
import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.application.services.LoginService;
import com.jeepclub.backend.authentication.core.domain.exception.CpfNotFoundException;
import com.jeepclub.backend.authentication.core.domain.exception.InvalidPasswordException;
import com.jeepclub.backend.infra.security.authorization.UserAuthoritiesProvider;
import com.jeepclub.backend.infra.security.jwt.JwtTokenParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(LoginController.class)
@AutoConfigureMockMvc(addFilters = false)
class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginService loginService;

    @MockitoBean
    private JwtTokenParser jwtTokenParser; // Necessário para contextos de segurança que podem interceptar

    @MockitoBean
    private UserAuthoritiesProvider userAuthoritiesProvider;

    @Test
    @DisplayName("Sucesso: Login com credenciais válidas retorna 200 e tokens")
    void shouldReturnTokensOnSuccess() throws Exception {
        AuthTokens tokens = new AuthTokens("refresh-xyz", "access-xyz", 3600L);
        when(loginService.login("52998224725", "senha123")).thenReturn(tokens);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\": \"52998224725\", \"senha\": \"senha123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-xyz"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-xyz"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600));
    }

    @Test
    @DisplayName("Falha: Senha incorreta retorna 401 Unauthorized")
    void shouldReturn401OnInvalidPassword() throws Exception {
        when(loginService.login(anyString(), anyString())).thenThrow(new InvalidPasswordException());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\": \"52998224725\", \"senha\": \"senhaErrada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").exists())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("Falha: Usuário inexistente retorna 404 Not Found")
    void shouldReturn404OnUserNotFound() throws Exception {
        when(loginService.login(anyString(), anyString())).thenThrow(new CpfNotFoundException("CPF não encontrado"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\": \"52998224725\", \"senha\": \"senha123\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("CPF não encontrado"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("Falha: Dados inválidos (ex: sem CPF ou senha) retorna 400 Bad Request")
    void shouldReturn400OnInvalidData() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\": \"\", \"senha\": \"\"}")) // CPF vazio e senha vazia
                .andExpect(status().isBadRequest());
    }
}
