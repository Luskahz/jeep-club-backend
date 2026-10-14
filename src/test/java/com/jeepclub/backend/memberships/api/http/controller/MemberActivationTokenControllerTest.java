package com.jeepclub.backend.memberships.api.http.controller;

import com.jeepclub.backend.memberships.api.http.exception.MembershipExceptionHandler;
import com.jeepclub.backend.memberships.core.application.exception.MemberActivationTokenAlreadyUsedException;
import com.jeepclub.backend.memberships.core.application.exception.MemberActivationTokenExpiredException;
import com.jeepclub.backend.memberships.core.application.exception.MemberActivationTokenNotFoundException;
import com.jeepclub.backend.memberships.core.application.service.memberactivationtoken.MemberActivationTokenService;
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
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MemberActivationTokenControllerTest {

    @Mock
    private MemberActivationTokenService memberActivationTokenService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        JsonMapper jsonMapper = JsonMapper.builder()
                .findAndAddModules()
                .disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
                .build();

        mockMvc = MockMvcBuilders.standaloneSetup(
                        new MemberActivationTokenController(memberActivationTokenService)
                )
                .setControllerAdvice(new MembershipExceptionHandler(), new GlobalExceptionHandler())
                .setMessageConverters(new JacksonJsonHttpMessageConverter(jsonMapper))
                .build();
    }

    @Test
    @DisplayName("GET /activate should return 204 No Content when token is valid")
    void shouldReturn204WhenTokenIsValid() throws Exception {
        doNothing().when(memberActivationTokenService).validate("valid-token");

        mockMvc.perform(get("/membership-applications/activate")
                        .param("token", "valid-token"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /activate should return 400 Bad Request when token is missing")
    void shouldReturn400WhenTokenIsMissing() throws Exception {
        mockMvc.perform(get("/membership-applications/activate"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /activate should return 404 Not Found RFC 9457 when token does not exist")
    void shouldReturn404WhenTokenNotFound() throws Exception {
        doThrow(new MemberActivationTokenNotFoundException())
                .when(memberActivationTokenService).validate("missing-token");

        mockMvc.perform(get("/membership-applications/activate")
                        .param("token", "missing-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ACTIVATION_TOKEN_NOT_FOUND"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /activate should return 410 Gone RFC 9457 when token is expired")
    void shouldReturn410WhenTokenExpired() throws Exception {
        doThrow(new MemberActivationTokenExpiredException())
                .when(memberActivationTokenService).validate("expired-token");

        mockMvc.perform(get("/membership-applications/activate")
                        .param("token", "expired-token"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("ACTIVATION_TOKEN_EXPIRED"))
                .andExpect(jsonPath("$.status").value(410));
    }

    @Test
    @DisplayName("GET /activate should return 409 Conflict RFC 9457 when token is already used")
    void shouldReturn409WhenTokenAlreadyUsed() throws Exception {
        doThrow(new MemberActivationTokenAlreadyUsedException())
                .when(memberActivationTokenService).validate("used-token");

        mockMvc.perform(get("/membership-applications/activate")
                        .param("token", "used-token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTIVATION_TOKEN_ALREADY_USED"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST /activate should return 204 No Content when completion succeeds")
    void shouldReturn204OnCompletionSuccess() throws Exception {
        doNothing().when(memberActivationTokenService).complete("valid-token", "strongPassword123!");

        String body = """
                {
                  "token": "valid-token",
                  "newPassword": "strongPassword123!"
                }
                """;

        mockMvc.perform(post("/membership-applications/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /activate should return 400 Bad Request when password is too short")
    void shouldReturn400OnInvalidBody() throws Exception {
        String body = """
                {
                  "token": "valid-token",
                  "newPassword": "short"
                }
                """;

        mockMvc.perform(post("/membership-applications/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /activate should return 410 Gone RFC 9457 when token has expired")
    void shouldReturn410OnCompletionExpired() throws Exception {
        doThrow(new MemberActivationTokenExpiredException())
                .when(memberActivationTokenService).complete("expired-token", "strongPassword123!");

        String body = """
                {
                  "token": "expired-token",
                  "newPassword": "strongPassword123!"
                }
                """;

        mockMvc.perform(post("/membership-applications/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("ACTIVATION_TOKEN_EXPIRED"));
    }

    @Test
    @DisplayName("POST /activate should return 409 Conflict RFC 9457 when token has already been consumed")
    void shouldReturn409OnCompletionUsed() throws Exception {
        doThrow(new MemberActivationTokenAlreadyUsedException())
                .when(memberActivationTokenService).complete("used-token", "strongPassword123!");

        String body = """
                {
                  "token": "used-token",
                  "newPassword": "strongPassword123!"
                }
                """;

        mockMvc.perform(post("/membership-applications/activate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ACTIVATION_TOKEN_ALREADY_USED"));
    }
}
