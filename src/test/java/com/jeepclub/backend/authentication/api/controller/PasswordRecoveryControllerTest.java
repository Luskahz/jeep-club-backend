package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.core.application.results.PasswordResetTokenAdminResult;
import com.jeepclub.backend.authentication.core.application.results.TemporaryPasswordAdminResult;
import com.jeepclub.backend.authentication.core.application.services.AccessTokenAuthenticationService;
import com.jeepclub.backend.authentication.core.application.services.PasswordRecoveryService;
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

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordRecoveryController.class)
@AutoConfigureMockMvc(addFilters = false)
class PasswordRecoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PasswordRecoveryService passwordRecoveryService;

    @MockitoBean
    private JwtTokenParser jwtTokenParser;

    @MockitoBean
    private UserAuthoritiesProvider userAuthoritiesProvider;

    @MockitoBean
    private AccessTokenAuthenticationService accessTokenAuthenticationService;

    @Test
    @DisplayName("Sucesso: Recuperação via e-mail retorna 202")
    void shouldReturnAcceptedForEmailRecovery() throws Exception {
        doNothing().when(passwordRecoveryService).requestRecoveryViaEmail(anyString());

        String payload = """
                {
                    "cpf": "52998224725"
                }
                """;

        mockMvc.perform(post("/auth/password-recovery/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("Sucesso: Admin gera senha temporária e retorna 200")
    void shouldReturnOkWhenAdminGeneratesTemporaryPassword() throws Exception {
        TemporaryPasswordAdminResult result = new TemporaryPasswordAdminResult("senhaTemp123!");

        when(passwordRecoveryService.generateTemporaryPasswordByAdmin(anyLong()))
                .thenReturn(result);

        mockMvc.perform(post("/auth/password-recovery/admin/users/{userId}/temporary-password", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporaryPassword").value("senhaTemp123!"));
    }

    @Test
    @DisplayName("Sucesso: Admin gera token de redefinição e retorna 200")
    void shouldReturnOkWhenAdminGeneratesResetToken() throws Exception {
        PasswordResetTokenAdminResult result = new PasswordResetTokenAdminResult("tokenAdmin123");

        when(passwordRecoveryService.generateResetTokenByAdmin(anyLong()))
                .thenReturn(result);

        mockMvc.perform(post("/auth/password-recovery/admin/users/{userId}/reset-token", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resetToken").value("tokenAdmin123"));
    }

    @Test
    @DisplayName("Sucesso: Efetivar troca de senha retorna 204")
    void shouldReturnNoContentOnPasswordReset() throws Exception {
        doNothing().when(passwordRecoveryService).resetPassword(anyString(), anyString());

        String payload = """
                {
                    "token": "tokenMagico",
                    "newPassword": "novaSenhaSuperForte"
                }
                """;

        mockMvc.perform(post("/auth/password-recovery/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isNoContent());
    }
}