package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.iam.authentication.api.http.controller.admin.AdminSessionController;
import com.jeepclub.backend.iam.authentication.api.http.exception.SessionExceptionHandler;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.session.SessionNotFoundException;
import com.jeepclub.backend.iam.authentication.core.application.result.admin.session.AdminSessionResult;
import com.jeepclub.backend.iam.authentication.core.application.service.session.AdminSessionService;
import com.jeepclub.backend.iam.authentication.core.domain.exception.session.SessionNotActiveException;
import com.jeepclub.backend.platform.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminSessionControllerTest {

    private static final String BASE_URL = "/authentication/admin";
    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");
    private static final Instant EXPIRES = Instant.parse("2026-10-15T20:00:00Z");

    @Mock
    private AdminSessionService adminSessionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JsonMapper jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        AdminSessionController controller = new AdminSessionController(adminSessionService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(
                        new GlobalExceptionHandler(),
                        new SessionExceptionHandler()
                )
                .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper))
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("Sucesso: lista todas as sessões cadastradas retornando 200")
    void shouldReturnAllSessions() throws Exception {
        AdminSessionResult session1 = new AdminSessionResult(1L, 10L, "ACTIVE", NOW, EXPIRES, null);
        AdminSessionResult session2 = new AdminSessionResult(2L, 20L, "LOGGED_OUT", NOW, EXPIRES, NOW.plusSeconds(3600));

        when(adminSessionService.findAll()).thenReturn(List.of(session1, session2));

        mockMvc.perform(get(BASE_URL + "/sessions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].userId").value(10L))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].status").value("LOGGED_OUT"));

        verify(adminSessionService).findAll();
    }

    @Test
    @DisplayName("Sucesso: consulta sessão existente por ID retornando 200")
    void shouldReturnSessionById() throws Exception {
        AdminSessionResult session = new AdminSessionResult(1L, 10L, "ACTIVE", NOW, EXPIRES, null);

        when(adminSessionService.findById(1L)).thenReturn(session);

        mockMvc.perform(get(BASE_URL + "/sessions/{sessionId}", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(10L))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(adminSessionService).findById(1L);
    }

    @Test
    @DisplayName("Falha: consulta de sessão inexistente retorna 404 via RFC 9457")
    void shouldReturnNotFoundWhenSessionDoesNotExist() throws Exception {
        when(adminSessionService.findById(999L))
                .thenThrow(new SessionNotFoundException("Session not found"));

        mockMvc.perform(get(BASE_URL + "/sessions/{sessionId}", 999L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Session not found"));
    }

    @Test
    @DisplayName("Sucesso: lista sessões por ID de usuário retornando 200")
    void shouldReturnSessionsByUserId() throws Exception {
        AdminSessionResult session = new AdminSessionResult(1L, 10L, "ACTIVE", NOW, EXPIRES, null);

        when(adminSessionService.findByUserId(10L)).thenReturn(List.of(session));

        mockMvc.perform(get(BASE_URL + "/users/{userId}/sessions", 10L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(10L));

        verify(adminSessionService).findByUserId(10L);
    }

    @Test
    @DisplayName("Sucesso: encerra administrativamente uma sessão retornando 200 com status LOGGED_OUT")
    void shouldLogoutSessionAdministratively() throws Exception {
        Instant logoutTime = NOW.plusSeconds(1800);
        AdminSessionResult loggedOutSession = new AdminSessionResult(1L, 10L, "LOGGED_OUT", NOW, EXPIRES, logoutTime);

        when(adminSessionService.logout(1L)).thenReturn(loggedOutSession);

        mockMvc.perform(patch(BASE_URL + "/sessions/{sessionId}/logout", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("LOGGED_OUT"))
                .andExpect(jsonPath("$.loggedOutAt").isNotEmpty());

        verify(adminSessionService).logout(1L);
    }

    @Test
    @DisplayName("Falha: encerramento de sessão com erro de estado lança 400 Bad Request")
    void shouldReturnBadRequestWhenLogoutFailsStateValidation() throws Exception {
        when(adminSessionService.logout(1L))
                .thenThrow(new SessionNotActiveException("Only active sessions can be revoked"));

        mockMvc.perform(patch(BASE_URL + "/sessions/{sessionId}/logout", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SESSION_NOT_ACTIVE"));
    }
}
