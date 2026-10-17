package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.iam.authentication.api.http.controller.RefreshTokenController;
import com.jeepclub.backend.iam.authentication.api.http.dto.token.RefreshTokenRequestDTO;
import com.jeepclub.backend.iam.authentication.api.http.exception.RefreshTokenExceptionHandler;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.refreshtoken.RefreshTokenInvalidException;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.refreshtoken.RefreshTokenNotFoundException;
import com.jeepclub.backend.iam.authentication.core.domain.exception.refreshtoken.RefreshTokenStateException;
import com.jeepclub.backend.iam.authentication.core.domain.exception.refreshtoken.RefreshTokenValidationException;
import com.jeepclub.backend.iam.authentication.core.application.result.AuthTokens;
import com.jeepclub.backend.iam.authentication.core.application.service.refreshtoken.RefreshTokenService;
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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RefreshTokenControllerTest {

    private static final String REFRESH_URL = "/authentication/refresh";
    private static final String VALID_RAW_TOKEN = "valid.refresh.token.payload";

    @Mock
    private RefreshTokenService refreshTokenService;

    private MockMvc mockMvc;
    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        RefreshTokenController controller = new RefreshTokenController(refreshTokenService);

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
    @DisplayName("Sucesso: renova tokens a partir de refresh token válido retornando 200")
    void shouldReturnOkAndTokensWhenRefreshTokenIsValid() throws Exception {
        AuthTokens tokens = new AuthTokens(
                "new.raw.refresh.token",
                "new.access.token.jwt",
                3600L
        );

        when(refreshTokenService.rotate(VALID_RAW_TOKEN)).thenReturn(tokens);

        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(VALID_RAW_TOKEN);

        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("new.raw.refresh.token"))
                .andExpect(jsonPath("$.accessToken").value("new.access.token.jwt"))
                .andExpect(jsonPath("$.expiresInSeconds").value(3600L));

        verify(refreshTokenService).rotate(VALID_RAW_TOKEN);
    }

    @Test
    @DisplayName("Falha: payload em branco ou nulo retorna 400 Bad Request")
    void shouldReturnBadRequestWhenRefreshTokenIsBlank() throws Exception {
        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO("   ");

        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Falha: token inválido, expirado ou revogado retorna 401 Unauthorized via RFC 9457")
    void shouldReturnUnauthorizedWhenRefreshTokenIsInvalid() throws Exception {
        when(refreshTokenService.rotate(anyString()))
                .thenThrow(new RefreshTokenInvalidException());

        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(VALID_RAW_TOKEN);

        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID"))
                .andExpect(jsonPath("$.detail").value("Refresh token is invalid or expired."));
    }

    @Test
    @DisplayName("Falha: erro de validação do domínio lança 400 com código REFRESH_TOKEN_INVALID_DATA")
    void shouldReturnBadRequestWhenDomainValidationFails() throws Exception {
        when(refreshTokenService.rotate(anyString()))
                .thenThrow(new RefreshTokenValidationException("tokenHash cannot exceed 255 characters"));

        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(VALID_RAW_TOKEN);

        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_INVALID_DATA"))
                .andExpect(jsonPath("$.detail").value("tokenHash cannot exceed 255 characters"));
    }

    @Test
    @DisplayName("Falha: conflito de estado lança 409 com código REFRESH_TOKEN_STATE_CONFLICT")
    void shouldReturnConflictWhenRefreshTokenStateIsConflicted() throws Exception {
        when(refreshTokenService.rotate(anyString()))
                .thenThrow(new RefreshTokenStateException("Refresh token has already been rotated."));

        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(VALID_RAW_TOKEN);

        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_STATE_CONFLICT"))
                .andExpect(jsonPath("$.detail").value("Refresh token has already been rotated."));
    }

    @Test
    @DisplayName("Falha: token não encontrado lança 404 com código REFRESH_TOKEN_NOT_FOUND")
    void shouldReturnNotFoundWhenRefreshTokenNotFound() throws Exception {
        when(refreshTokenService.rotate(anyString()))
                .thenThrow(new RefreshTokenNotFoundException("Refresh token not found"));

        RefreshTokenRequestDTO request = new RefreshTokenRequestDTO(VALID_RAW_TOKEN);

        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_NOT_FOUND"));
    }
}
