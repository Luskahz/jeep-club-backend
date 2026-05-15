package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.application.services.LoginService;
import com.jeepclub.backend.authentication.core.application.services.RegisterService;
import com.jeepclub.backend.authentication.core.domain.model.User;
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

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RegisterController.class)
@AutoConfigureMockMvc(addFilters = false)
class RegisterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RegisterService registerService;

    @MockitoBean
    private LoginService loginService;

    @MockitoBean
    private JwtTokenParser jwtTokenParser; // Necessário para contextos de segurança que podem interceptar

    @MockitoBean
    private UserAuthoritiesProvider userAuthoritiesProvider;

    @Test
    @DisplayName("Sucesso: Registro com dados válidos retorna 201 e tokens")
    void shouldReturnTokensOnSuccessfulRegistration() throws Exception {
        User mockedUser = User.create("Teste", LocalDate.of(1990, 1, 1), "teste@email.com", "52998224725", "1234567", "hash", "11999999999", java.time.Instant.now());
        AuthTokens tokens = new AuthTokens("refresh-reg", "access-reg", 3600L);

        when(registerService.registerUser(anyString(), any(LocalDate.class), anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockedUser);
        
        when(loginService.login("52998224725", "senha123"))
                .thenReturn(tokens);

        String payload = """
                {
                  "name": "Teste",
                  "birthData": "1990-01-01",
                  "email": "teste@email.com",
                  "cpf": "52998224725",
                  "rg": "1234567",
                  "password": "senha123",
                  "phoneNumber": "11999999999"
                }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-reg"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-reg"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600));
    }

    @Test
    @DisplayName("Falha: Dados inválidos retorna 400 Bad Request")
    void shouldReturn400OnInvalidData() throws Exception {
        String invalidPayload = """
                {
                  "name": "",
                  "cpf": "123",
                  "password": ""
                }
                """;

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }
}
