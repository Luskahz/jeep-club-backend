package com.jeepclub.backend.authentication.core.application.service;

import com.jeepclub.backend.authentication.core.domain.enums.AccountStatus;
import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationStatus;
import com.jeepclub.backend.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.authentication.core.domain.enums.SessionStatus;
import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.authentication.infra.persistence.entity.UserEntity;
import com.jeepclub.backend.authentication.infra.persistence.jpa.RefreshTokenJpaRepository;
import com.jeepclub.backend.authentication.infra.persistence.jpa.SessionJpaRepository;
import com.jeepclub.backend.authentication.infra.persistence.jpa.UserJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConcurrentLoginIntegrationTest {

    private static final String CPF = "52998224725";
    private static final String PASSWORD = "Senha@123";

    @Autowired
    private LoginService loginService;
    @Autowired
    private PasswordHasher passwordHasher;
    @Autowired
    private UserJpaRepository userRepository;
    @Autowired
    private SessionJpaRepository sessionRepository;
    @Autowired
    private RefreshTokenJpaRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        clearAuthenticationData();
        UserEntity user = new UserEntity();
        user.setName("Concurrent User");
        user.setCpf(CPF);
        user.setPasswordHash(passwordHasher.hash(PASSWORD));
        user.setAccountStatus(AccountStatus.ACTIVE);
        user.setAuthenticationStatus(AuthenticationStatus.ENABLED);
        user.setCredentialStatus(CredentialStatus.PERMANENT);
        user.setCreatedAt(Instant.parse("2026-06-22T12:00:00Z"));
        userRepository.saveAndFlush(user);
    }

    @AfterEach
    void tearDown() {
        clearAuthenticationData();
    }

    @Test
    void simultaneousLoginsCreateOnlyOneActiveSession() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> loginAfter(start));
            Future<?> second = executor.submit(() -> loginAfter(start));
            start.countDown();

            first.get(10, TimeUnit.SECONDS);
            second.get(10, TimeUnit.SECONDS);

            Long userId = userRepository.findByCpf(CPF).orElseThrow().getId();
            long activeSessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                    .stream()
                    .filter(session -> session.getStatus() == SessionStatus.ACTIVE)
                    .count();
            assertThat(activeSessions).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    private void loginAfter(CountDownLatch start) {
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent login start timed out");
            }
            loginService.login(CPF, PASSWORD);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent login interrupted", exception);
        }
    }

    private void clearAuthenticationData() {
        refreshTokenRepository.deleteAll();
        sessionRepository.deleteAll();
        userRepository.deleteAll();
    }
}
