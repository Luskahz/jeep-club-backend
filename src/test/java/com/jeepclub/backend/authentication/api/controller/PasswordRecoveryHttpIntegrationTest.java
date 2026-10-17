package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.iam.authentication.core.domain.enums.PasswordRecoveryRequestStatus;
import com.jeepclub.backend.iam.authentication.core.port.NotificationPort;
import com.jeepclub.backend.iam.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.iam.authentication.core.port.RefreshTokenHashService;
import com.jeepclub.backend.iam.authentication.infra.persistence.jpa.*;
import com.jeepclub.backend.iam.identity.api.module.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PasswordRecoveryHttpIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRegistration registration;
    @Autowired UserQuery identities;
    @Autowired PasswordRecoveryRequestJpaRepository recovery;
    @Autowired AuthenticationAccountJpaRepository accounts;
    @Autowired RefreshTokenHashService hashes;
    @Autowired PasswordHasher passwords;
    @MockitoBean NotificationPort notification;

    @Test
    void emailResetChangesPersistedPasswordRevokesOldCredentialsAndRejectsReuse() throws Exception {
        var login = register("52998224725");
        Long id = identities.findByCpf("52998224725").orElseThrow().id();
        String raw = requestToken("52998224725");
        var request = recovery.findByTokenHash(hashes.hash(raw)).orElseThrow();
        assertThat(request.getTokenHash()).isNotEqualTo(raw);
        reset(raw).andExpect(status().isNoContent());
        assertThat(recovery.findById(request.getId()).orElseThrow().getStatus())
                .isEqualTo(PasswordRecoveryRequestStatus.RESOLVED);
        assertThat(passwords.matches("NewPassword@123", accounts.findById(id).orElseThrow().getPasswordHash())).isTrue();
        mvc.perform(get("/authentication/me").header("Authorization", "Bearer " + login.accessToken()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/authentication/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + login.refreshToken() + "\"}"))
                .andExpect(status().isUnauthorized());
        reset(raw).andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
        String newAccess = com.jayway.jsonpath.JsonPath.read(mvc.perform(post("/authentication/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"52998224725\",\"senha\":\"NewPassword@123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AUTHENTICATED"))
                .andReturn().getResponse().getContentAsString(), "$.accessToken");
        mvc.perform(get("/authentication/me").header("Authorization", "Bearer " + newAccess))
                .andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(id));
    }

    @Test
    void expiredPersistedRecoveryTokenCannotChangePasswordOrRevokeValidSession() throws Exception {
        var login = register("16899535009");
        Long id = identities.findByCpf("16899535009").orElseThrow().id();
        String before = accounts.findById(id).orElseThrow().getPasswordHash();
        String raw = requestToken("16899535009");
        var request = recovery.findByTokenHash(hashes.hash(raw)).orElseThrow();
        request.setExpiresAt(Instant.now());
        recovery.saveAndFlush(request);
        reset(raw).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("TOKEN_INVALID"));
        assertThat(accounts.findById(id).orElseThrow().getPasswordHash()).isEqualTo(before);
        assertThat(recovery.findById(request.getId()).orElseThrow().getStatus()).isEqualTo(PasswordRecoveryRequestStatus.OPEN);
        mvc.perform(get("/authentication/me").header("Authorization", "Bearer " + login.accessToken()))
                .andExpect(status().isOk());
    }

    private UserAuthenticationTokens register(String cpf) {
        return registration.registerAndAuthenticate(new UserRegistrationData("Recovery Audit", null,
                cpf + "@example.com", cpf, null, null, null, Instant.now()), "OldPassword@123");
    }

    private String requestToken(String cpf) throws Exception {
        mvc.perform(post("/authentication/password-recovery/requests/email-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cpf\":\"" + cpf + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").doesNotExist());
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(notification).sendPasswordResetLink(eq(cpf + "@example.com"), link.capture());
        return UriComponentsBuilder.fromUriString(link.getValue()).build().getQueryParams().getFirst("token");
    }

    private org.springframework.test.web.servlet.ResultActions reset(String raw) throws Exception {
        return mvc.perform(post("/authentication/password-recovery/requests/token/reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"" + raw + "\",\"newPassword\":\"NewPassword@123\"}"));
    }
}
