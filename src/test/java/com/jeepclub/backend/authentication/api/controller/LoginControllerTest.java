package com.jeepclub.backend.authentication.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.jeepclub.backend.authentication.api.http.controller.SessionController;
import com.jeepclub.backend.authentication.api.http.exception.PasswordChangeChallengeExceptionHandler;
import com.jeepclub.backend.authentication.api.http.exception.SessionExceptionHandler;
import com.jeepclub.backend.authentication.api.http.exception.UserExceptionHandler;
import com.jeepclub.backend.authentication.core.application.exceptions.login.InvalidCredentialsException;
import com.jeepclub.backend.authentication.core.application.result.AuthTokens;
import com.jeepclub.backend.authentication.core.application.result.MeResult;
import com.jeepclub.backend.authentication.core.application.result.login.AuthenticatedLoginResult;
import com.jeepclub.backend.authentication.core.application.result.login.PasswordChangeRequiredLoginResult;
import com.jeepclub.backend.authentication.core.application.service.session.SessionService;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.time.Instant;
import java.util.Arrays;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @Mock
    private SessionService sessionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        SessionController controller = new SessionController(sessionService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(
                        new GlobalExceptionHandler(),
                        new UserExceptionHandler(),
                        new SessionExceptionHandler(),
                        new PasswordChangeChallengeExceptionHandler()
                )
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .setValidator(validator)
                .build();
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Sucesso: login com credenciais definitivas retorna 200 e tokens")
    void shouldReturnTokensOnSuccessfulLogin() throws Exception {
        AuthTokens tokens = new AuthTokens(
                "refresh-xyz",
                "access-xyz",
                3600L
        );

        when(sessionService.login("52998224725", "senha123"))
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
    @DisplayName("Sucesso: login com senha provisÃ³ria retorna desafio de troca de senha")
    void shouldReturnPasswordChangeRequiredWhenTemporaryPasswordIsUsed() throws Exception {
        Instant expiresAt = Instant.parse("2026-05-21T20:30:00Z");

        when(sessionService.login("52998224725", "senhaProvisoria123"))
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
    @DisplayName("Falha: credenciais invÃ¡lidas retornam 401 Unauthorized")
    void shouldReturnUnauthorizedOnInvalidCredentials() throws Exception {
        when(sessionService.login("52998224725", "senhaErrada"))
                .thenThrow(new InvalidCredentialsException());

        String payload = """
                {
                    "cpf": "52998224725",
                    "senha": "senhaErrada"
                }
                """;

        mockMvc.perform(post("/authentication/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("Sucesso: consultar sessÃ£o autenticada usa o principal e ordena authorities")
    void shouldReturnAuthenticatedSessionData() throws Exception {
        Authentication authentication = authenticatedUser(
                "AUTHENTICATION_USER_READ",
                "AUTHENTICATION_SESSION_READ"
        );

        when(sessionService.getCurrentSession(
                1L,
                10L,
                Instant.parse("2026-05-21T20:30:00Z")
        )).thenReturn(new MeResult(
                1L,
                "Lucas Alves",
                10L,
                true,
                900L
        ));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        mockMvc.perform(get("/authentication/me")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1L))
                .andExpect(jsonPath("$.userName").value("Lucas Alves"))
                .andExpect(jsonPath("$.sessionId").value(10L))
                .andExpect(jsonPath("$.sessionActive").value(true))
                .andExpect(jsonPath("$.expiresInSeconds").value(900L))
                .andExpect(jsonPath("$.authorities[0]").value("AUTHENTICATION_SESSION_READ"))
                .andExpect(jsonPath("$.authorities[1]").value("AUTHENTICATION_USER_READ"));
    }

    @Test
    @DisplayName("Sucesso: logout usa os identificadores do principal autenticado")
    void shouldLogoutUsingAuthenticatedPrincipal() throws Exception {
        Authentication authentication = authenticatedUser(
                "AUTHENTICATION_SESSION_LOGOUT"
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        mockMvc.perform(post("/authentication/logout")
                        .principal(authentication))
                .andExpect(status().isNoContent());

        verify(sessionService).logout(1L, 10L);
    }

    @Test
    @DisplayName("Falha: dados invÃ¡lidos retornam 400 Bad Request")
    void shouldReturnBadRequestOnInvalidData() throws Exception {
        String payload = """
                {
                    "cpf": "",
                    "senha": ""
                }
                """;

        mockMvc.perform(post("/authentication/login")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.detail").value("Existem campos inválidos na requisição."))
                .andExpect(jsonPath("$.errors[0].field").exists())
                .andExpect(jsonPath("$.errors[0].code").exists())
                .andExpect(jsonPath("$.errors[0].rejectedValue").doesNotExist());
    }

    private Authentication authenticatedUser(String... authorities) {
        UserPrincipal principal = new UserPrincipal(
                1L,
                10L,
                Instant.parse("2026-05-21T20:30:00Z")
        );

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                Arrays.stream(authorities)
                        .map(SimpleGrantedAuthority::new)
                        .toList()
        );
    }
}
