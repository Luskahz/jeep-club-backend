package com.jeepclub.backend.authentication.core.application.service;

import com.jeepclub.backend.iam.authentication.core.application.exceptions.refreshtoken.RefreshTokenInvalidException;
import com.jeepclub.backend.iam.authentication.core.application.result.AuthTokens;
import com.jeepclub.backend.iam.authentication.core.application.service.refreshtoken.RefreshTokenService;
import com.jeepclub.backend.iam.authentication.core.application.service.session.SessionService;
import com.jeepclub.backend.iam.authentication.core.domain.enums.RefreshTokenStatus;
import com.jeepclub.backend.iam.authentication.core.domain.enums.SessionStatus;
import com.jeepclub.backend.iam.authentication.core.port.JwtService;
import com.jeepclub.backend.iam.authentication.core.port.RefreshTokenHashService;
import com.jeepclub.backend.iam.authentication.infra.persistence.entity.RefreshTokenEntity;
import com.jeepclub.backend.iam.authentication.infra.persistence.jpa.RefreshTokenJpaRepository;
import com.jeepclub.backend.iam.authentication.infra.persistence.jpa.SessionJpaRepository;
import com.jeepclub.backend.iam.identity.api.module.UserRegistration;
import com.jeepclub.backend.iam.identity.api.module.UserRegistrationData;
import com.jeepclub.backend.iam.identity.api.module.UserAuthenticationTokens;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Real transactions, JWT and H2 locks. Only JWT failure is injected to test rollback. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RefreshTokenTransactionIntegrationTest {
    @Autowired UserRegistration registration;
    @Autowired RefreshTokenService refresh;
    @Autowired SessionService sessions;
    @Autowired RefreshTokenJpaRepository tokens;
    @Autowired SessionJpaRepository sessionRows;
    @Autowired RefreshTokenHashService hashes;
    @Autowired MockMvc mvc;
    @Autowired java.time.Clock clock;
    @MockitoSpyBean JwtService jwt;

    @Test
    void simultaneousRotationCommitsExactlyOneSuccessorAndRejectsReplay() throws Exception {
        UserAuthenticationTokens login = register("52998224725");
        RefreshTokenEntity original = token(login.refreshToken());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Callable<AuthTokens> rotate = () -> {
                ready.countDown();
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                try {
                    return refresh.rotate(login.refreshToken());
                } catch (RefreshTokenInvalidException expected) {
                    return null;
                }
            };
            Future<AuthTokens> first = executor.submit(rotate);
            Future<AuthTokens> second = executor.submit(rotate);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            List<AuthTokens> successes = java.util.stream.Stream.of(
                    first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS))
                    .filter(java.util.Objects::nonNull).toList();
            assertThat(successes).hasSize(1);
            RefreshTokenEntity successor = token(successes.get(0).refreshToken());
            RefreshTokenEntity consumed = tokens.findById(original.getId()).orElseThrow();
            assertThat(consumed.getStatus()).isEqualTo(RefreshTokenStatus.ROTATED);
            assertThat(consumed.getReplacedByTokenId()).isEqualTo(successor.getId());
            assertThat(successor.getSessionId()).isEqualTo(original.getSessionId());
            assertThat(successor.getTokenHash()).isNotEqualTo(successes.get(0).refreshToken());
            assertThat(rows(original.getSessionId())).hasSize(2);
            assertThatThrownBy(() -> refresh.rotate(login.refreshToken()))
                    .isInstanceOf(RefreshTokenInvalidException.class);
            mvc.perform(get("/authentication/me").header("Authorization",
                    "Bearer " + successes.get(0).accessToken())).andExpect(status().isOk());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void concurrentLogoutAndRotationLeaveNoUsableRefreshTokenOrSession() throws Exception {
        UserAuthenticationTokens login = register("16899535009");
        RefreshTokenEntity original = token(login.refreshToken());
        Long userId = sessionRows.findById(original.getSessionId()).orElseThrow().getUserId();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<AuthTokens> rotation = executor.submit(() -> {
                ready.countDown();
                assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
                try { return refresh.rotate(login.refreshToken()); }
                catch (RefreshTokenInvalidException expected) { return null; }
            });
            Future<?> logout = executor.submit(() -> {
                ready.countDown();
                try {
                    if (!start.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("start timeout");
                    sessions.logout(userId, original.getSessionId());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
            });
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            AuthTokens rotated = rotation.get(20, TimeUnit.SECONDS);
            logout.get(20, TimeUnit.SECONDS);
            assertThat(sessionRows.findById(original.getSessionId()).orElseThrow().getStatus())
                    .isEqualTo(SessionStatus.LOGGED_OUT);
            assertThat(rows(original.getSessionId())).noneMatch(t -> t.getStatus() == RefreshTokenStatus.ACTIVE);
            assertThatThrownBy(() -> refresh.rotate(login.refreshToken()))
                    .isInstanceOf(RefreshTokenInvalidException.class);
            if (rotated != null) {
                assertThatThrownBy(() -> refresh.rotate(rotated.refreshToken()))
                        .isInstanceOf(RefreshTokenInvalidException.class);
            }
            mvc.perform(get("/authentication/me").header("Authorization", "Bearer " + login.accessToken()))
                    .andExpect(status().isUnauthorized());
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void failedJwtIssuanceRollsBackSuccessorAndKeepsOriginalUsable() {
        UserAuthenticationTokens login = register("11144477735");
        RefreshTokenEntity original = token(login.refreshToken());
        doThrow(new IllegalStateException("injected JWT failure")).when(jwt)
                .generateAccessToken(anyLong(), anyString(), any());
        try {
            assertThatThrownBy(() -> refresh.rotate(login.refreshToken()))
                    .isInstanceOf(IllegalStateException.class).hasMessage("injected JWT failure");
        } finally { reset(jwt); }
        RefreshTokenEntity persisted = tokens.findById(original.getId()).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(RefreshTokenStatus.ACTIVE);
        assertThat(persisted.getReplacedByTokenId()).isNull();
        assertThat(rows(original.getSessionId())).hasSize(1);
        assertThat(refresh.rotate(login.refreshToken()).refreshToken()).isNotBlank();
    }

    private UserAuthenticationTokens register(String cpf) {
        return registration.registerAndAuthenticate(new UserRegistrationData(
                "Refresh Audit", null, null, cpf, null, null, null,
                Instant.now(clock)), "AuditPassword@123");
    }

    private RefreshTokenEntity token(String raw) {
        return tokens.findByTokenHash(hashes.hash(raw)).orElseThrow();
    }

    private List<RefreshTokenEntity> rows(Long sessionId) {
        return tokens.findAll().stream().filter(t -> t.getSessionId().equals(sessionId)).toList();
    }
}
