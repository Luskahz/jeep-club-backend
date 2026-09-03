package com.jeepclub.backend.authentication.core.application.service;

import com.jeepclub.backend.authentication.core.application.service.session.SessionService;
import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationAccessStatus;
import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationStatus;
import com.jeepclub.backend.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.authentication.core.domain.enums.SessionStatus;
import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.authentication.infra.persistence.entity.AuthenticationAccountEntity;
import com.jeepclub.backend.authentication.infra.persistence.jpa.AuthenticationAccountJpaRepository;
import com.jeepclub.backend.authentication.infra.persistence.jpa.RefreshTokenJpaRepository;
import com.jeepclub.backend.authentication.infra.persistence.jpa.SessionJpaRepository;
import com.jeepclub.backend.identity.core.domain.enums.IdentityStatus;
import com.jeepclub.backend.identity.infra.persistence.entity.IdentityEntity;
import com.jeepclub.backend.identity.infra.persistence.jpa.IdentityJpaRepository;
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
    private SessionService sessionService;
    @Autowired
    private PasswordHasher passwordHasher;
    @Autowired
    private IdentityJpaRepository identityRepository;
    @Autowired
    private AuthenticationAccountJpaRepository accountRepository;
    @Autowired
    private SessionJpaRepository sessionRepository;
    @Autowired
    private RefreshTokenJpaRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        clearAuthenticationData();
        Instant createdAt = Instant.parse("2026-06-22T12:00:00Z");
        IdentityEntity identity = new IdentityEntity();
        identity.setName("Concurrent User");
        identity.setCpf(CPF);
        identity.setStatus(IdentityStatus.ACTIVE);
        identity.setCreatedAt(createdAt);
        identity = identityRepository.saveAndFlush(identity);
        AuthenticationAccountEntity account = new AuthenticationAccountEntity();
        account.setIdentityId(identity.getId());
        account.setPasswordHash(passwordHasher.hash(PASSWORD));
        account.setAccessStatus(AuthenticationAccessStatus.ENABLED);
        account.setAuthenticationStatus(AuthenticationStatus.ENABLED);
        account.setCredentialStatus(CredentialStatus.PERMANENT);
        account.setCreatedAt(createdAt);
        accountRepository.saveAndFlush(account);
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

            Long userId = identityRepository.findByCpf(CPF).orElseThrow().getId();
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
            sessionService.login(CPF, PASSWORD);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent login interrupted", exception);
        }
    }

    private void clearAuthenticationData() {
        refreshTokenRepository.deleteAll();
        sessionRepository.deleteAll();
        accountRepository.deleteAll();
        identityRepository.deleteAll();
    }
}
