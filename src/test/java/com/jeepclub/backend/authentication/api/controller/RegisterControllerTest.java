package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.application.services.AccessTokenAuthenticationService;
import com.jeepclub.backend.authentication.core.application.services.LoginService;
import com.jeepclub.backend.authentication.core.application.services.RegisterService;
import com.jeepclub.backend.authentication.core.domain.model.User;
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
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    private JwtTokenParser jwtTokenParser;

    @MockitoBean
    private UserAuthoritiesProvider userAuthoritiesProvider;

    @MockitoBean
    private AccessTokenAuthenticationService accessTokenAuthenticationService;

    @Test
    @DisplayName("Sucesso: registro com dados válidos retorna 201 e tokens")
    void shouldReturnTokensOnSuccessfulRegistration() throws Exception {
        User mockedUser = User.create(
                "Teste",
                LocalDate.of(1990, 1, 1),
                "teste@email.com",
                "52998224725",
                "1234567",
                "hash",
                "11999999999",
                Instant.parse("2026-05-21T18:00:00Z")
        );

        AuthTokens tokens = new AuthTokens(
                "refresh-reg",
                "access-reg",
                3600L
        );

        when(registerService.registerUser(
                anyString(),
                any(LocalDate.class),
                anyString(),
                anyString(),
                anyString(),
                anyString(),
                anyString()
        )).thenReturn(mockedUser);

        when(loginService.authenticateRegisteredUser(eq(mockedUser)))
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

        mockMvc.perform(post("/authentication/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("access-reg"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-reg"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600));
    }

    @Test
    @DisplayName("Falha: dados inválidos retorna 400 Bad Request")
    void shouldReturnBadRequestOnInvalidData() throws Exception {
        String invalidPayload = """
                {
                  "name": "",
                  "cpf": "123",
                  "password": ""
                }
                """;

        mockMvc.perform(post("/authentication/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPayload))
                .andExpect(status().isBadRequest());
    }
}