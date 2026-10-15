package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.iam.authentication.api.http.controller.admin.AdminRefreshTokenController;
import com.jeepclub.backend.iam.authentication.api.http.exception.RefreshTokenExceptionHandler;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.refreshtoken.RefreshTokenNotFoundException;
import com.jeepclub.backend.iam.authentication.core.application.result.admin.refresh.AdminRefreshTokenResult;
import com.jeepclub.backend.iam.authentication.core.application.service.refreshtoken.AdminRefreshTokenService;
import com.jeepclub.backend.iam.authentication.core.domain.exception.refreshtoken.RefreshTokenStateException;
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
class AdminRefreshTokenControllerTest {

    private static final String BASE_URL = "/authentication/admin";
    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");
    private static final Instant EXPIRES = Instant.parse("2026-10-22T12:00:00Z");

    @Mock
    private AdminRefreshTokenService adminRefreshTokenService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JsonMapper jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        AdminRefreshTokenController controller = new AdminRefreshTokenController(adminRefreshTokenService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(
                        new GlobalExceptionHandler(),
                        new RefreshTokenExceptionHandler()
                )
                .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper))
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("Sucesso: lista todos os refresh tokens retornando 200")
    void shouldReturnAllRefreshTokens() throws Exception {
        AdminRefreshTokenResult token1 = new AdminRefreshTokenResult(1L, 10L, 100L, "ACTIVE", NOW, EXPIRES, null);
        AdminRefreshTokenResult token2 = new AdminRefreshTokenResult(2L, 20L, 200L, "REVOKED", NOW, EXPIRES, null);

        when(adminRefreshTokenService.findAll()).thenReturn(List.of(token1, token2));

        mockMvc.perform(get(BASE_URL + "/refresh-tokens")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].userId").value(10L))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].status").value("REVOKED"));

        verify(adminRefreshTokenService).findAll();
    }

    @Test
    @DisplayName("Sucesso: consulta refresh token existente por ID retornando 200")
    void shouldReturnRefreshTokenById() throws Exception {
        AdminRefreshTokenResult token = new AdminRefreshTokenResult(1L, 10L, 100L, "ACTIVE", NOW, EXPIRES, null);

        when(adminRefreshTokenService.findById(1L)).thenReturn(token);

        mockMvc.perform(get(BASE_URL + "/refresh-tokens/{refreshTokenId}", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.userId").value(10L))
                .andExpect(jsonPath("$.sessionId").value(100L))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        verify(adminRefreshTokenService).findById(1L);
    }

    @Test
    @DisplayName("Falha: consulta de refresh token inexistente retorna 404 via RFC 9457")
    void shouldReturnNotFoundWhenRefreshTokenDoesNotExist() throws Exception {
        when(adminRefreshTokenService.findById(999L))
                .thenThrow(new RefreshTokenNotFoundException("Refresh token not found"));

        mockMvc.perform(get(BASE_URL + "/refresh-tokens/{refreshTokenId}", 999L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Refresh token not found"));
    }

    @Test
    @DisplayName("Sucesso: lista refresh tokens por ID de usuário retornando 200")
    void shouldReturnRefreshTokensByUserId() throws Exception {
        AdminRefreshTokenResult token = new AdminRefreshTokenResult(1L, 10L, 100L, "ACTIVE", NOW, EXPIRES, null);

        when(adminRefreshTokenService.findByUserId(10L)).thenReturn(List.of(token));

        mockMvc.perform(get(BASE_URL + "/users/{userId}/refresh-tokens", 10L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(10L));

        verify(adminRefreshTokenService).findByUserId(10L);
    }

    @Test
    @DisplayName("Sucesso: revoga administrativamente um refresh token retornando 200 com status REVOKED")
    void shouldRevokeRefreshTokenAdministratively() throws Exception {
        AdminRefreshTokenResult revokedToken = new AdminRefreshTokenResult(1L, 10L, 100L, "REVOKED", NOW, EXPIRES, null);

        when(adminRefreshTokenService.revoke(1L)).thenReturn(revokedToken);

        mockMvc.perform(patch(BASE_URL + "/refresh-tokens/{refreshTokenId}/revoke", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.status").value("REVOKED"));

        verify(adminRefreshTokenService).revoke(1L);
    }

    @Test
    @DisplayName("Falha: revogação com conflito de estado lança 409 Conflict")
    void shouldReturnConflictWhenRevocationFailsStateCheck() throws Exception {
        when(adminRefreshTokenService.revoke(1L))
                .thenThrow(new RefreshTokenStateException("Only active refresh tokens can be revoked."));

        mockMvc.perform(patch(BASE_URL + "/refresh-tokens/{refreshTokenId}/revoke", 1L)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_STATE_CONFLICT"))
                .andExpect(jsonPath("$.detail").value("Only active refresh tokens can be revoked."));
    }
}
