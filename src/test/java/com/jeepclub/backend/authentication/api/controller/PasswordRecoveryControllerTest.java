package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.api.dto.recovery.PasswordRecoveryAdminRequestDTO;
import com.jeepclub.backend.authentication.api.dto.recovery.PasswordRecoveryRequestDTO;
import com.jeepclub.backend.authentication.api.dto.recovery.PasswordResetDTO;
import com.jeepclub.backend.authentication.core.application.results.PasswordRecoveryAdminResult;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PasswordRecoveryController.class)
@AutoConfigureMockMvc(addFilters = false) // Desabilita os filtros de segurança para testar apenas o Controller
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
    @DisplayName("Sucesso: Recuperação via Email (sempre retorna 202)")
    void shouldReturnAcceptedForEmailRecovery() throws Exception {
        doNothing().when(passwordRecoveryService).requestRecoveryViaEmail(anyString());

        String payload = """
                {
                    "cpf": "52998224725"
                }
                """;

        mockMvc.perform(post("/auth/recovery/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("Sucesso: Recuperação via Admin retorna 200 e resultado")
    void shouldReturnOkAndResultForAdminRecovery() throws Exception {
        PasswordRecoveryAdminResult mockResult = new PasswordRecoveryAdminResult("senhaTemp123!", null);
        when(passwordRecoveryService.requestRecoveryViaAdmin(anyLong(), anyBoolean())).thenReturn(mockResult);

        String payload = """
                {
                    "targetUserId": 1,
                    "generateTempPassword": true
                }
                """;

        mockMvc.perform(post("/auth/recovery/admin-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.temporaryPassword").value("senhaTemp123!"));
    }

    @Test
    @DisplayName("Sucesso: Efetivar troca de senha retorna 200")
    void shouldReturnOkOnPasswordReset() throws Exception {
        doNothing().when(passwordRecoveryService).resetPassword(anyString(), anyString());

        String payload = """
                {
                    "token": "tokenMagico",
                    "newPassword": "novaSenhaSuperForte"
                }
                """;

        mockMvc.perform(post("/auth/recovery/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());
    }
}
