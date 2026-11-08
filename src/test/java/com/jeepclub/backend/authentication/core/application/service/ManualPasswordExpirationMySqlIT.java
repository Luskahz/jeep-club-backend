package com.jeepclub.backend.authentication.core.application.service;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import java.sql.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
@Testcontainers
class ManualPasswordExpirationMySqlIT extends ManualPasswordExpirationIntegrationTest {
    @Container static final MySQLContainer DB = new MySQLContainer("mysql:8.0.46").withUsername("root");
    @DynamicPropertySource static void database(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", DB::getJdbcUrl); r.add("spring.datasource.username", DB::getUsername);
        r.add("spring.datasource.password", DB::getPassword);
        r.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        r.add("spring.flyway.enabled", () -> "true"); r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }
    @Test void loginWaitingOnAccountLockRechecksExpiry() throws Exception {
        Fixture f = manual();
        withBlockedAccount(f, () -> challenge(f), () -> time.set(expiry(f)),
            com.jeepclub.backend.iam.authentication.core.application.exceptions.login.InvalidCredentialsException.class);
    }
    @Test void completionWaitingOnAccountLockCannotCrossExpiry() throws Exception {
        Fixture f = manual(); time.set(expiry(f).minusSeconds(30)); var c = challenge(f);
        withBlockedAccount(f, () -> sessions.completeRequiredPasswordChange(c.passwordChangeToken(), "NewPassword@123"),
            () -> time.set(expiry(f)), com.jeepclub.backend.iam.authentication.core.application.exceptions.login.PasswordChangeChallengeInvalidException.class);
    }
    @Test void reissueWinsAgainstWaitingOldChallenge() throws Exception {
        Fixture f = manual(); var c = challenge(f);
        var executor = Executors.newSingleThreadExecutor();
        try {
            transaction.executeWithoutResult(tx -> {
                jdbc.queryForObject("select id from identity_users where id=? for update", Long.class, f.id());
                domainAccounts.findByIdentityIdForUpdate(f.id()).orElseThrow();
                Future<?> pending = executor.submit(() -> sessions.completeRequiredPasswordChange(c.passwordChangeToken(), "NewPassword@123"));
                awaitLock();
                recovery.generateTemporaryPassword(f.id());
                pendingResult = pending;
            });
            assertThatThrownBy(() -> pendingResult.get(15, TimeUnit.SECONDS)).hasCauseInstanceOf(
                com.jeepclub.backend.iam.authentication.core.application.exceptions.login.PasswordChangeChallengeInvalidException.class);
            assertThat(expiry(f)).isNotNull();
        } finally { executor.shutdownNow(); }
    }
    @Test void completionWinsThenWaitingReissueCreatesNewManualCredential() throws Exception {
        Fixture f = manual(); var c = challenge(f);
        var executor = Executors.newSingleThreadExecutor();
        try {
            transaction.executeWithoutResult(tx -> {
                jdbc.queryForObject("select id from identity_users where id=? for update", Long.class, f.id());
                domainAccounts.findByIdentityIdForUpdate(f.id()).orElseThrow();
                pendingResult = executor.submit(() -> recovery.generateTemporaryPassword(f.id()));
                awaitLock();
                sessions.completeRequiredPasswordChange(c.passwordChangeToken(), "NewPassword@123");
            });
            var result = (com.jeepclub.backend.iam.authentication.core.application.result.TemporaryPasswordAdminResult) pendingResult.get(15, TimeUnit.SECONDS);
            challenge(new Fixture(f.id(), f.cpf(), result.temporaryPassword()));
            assertThat(accounts.findById(f.id()).orElseThrow().getCredentialStatus()).isEqualTo(
                com.jeepclub.backend.iam.authentication.core.domain.enums.CredentialStatus.CHANGE_REQUIRED);
        } finally { executor.shutdownNow(); }
    }
    @Test void completionRechecksExpiryAfterRecoveryRequestLock() throws Exception {
        Fixture initial = manual();
        String raw = recovery.generateTemporaryPassword(initial.id()).temporaryPassword();
        Fixture f = new Fixture(initial.id(), initial.cpf(), raw);
        time.set(expiry(f).minusSeconds(30)); var c = challenge(f);
        withBlockedRow(f, "select id from authentication_password_recovery_requests where user_id=? for update",
            () -> sessions.completeRequiredPasswordChange(c.passwordChangeToken(), "NewPassword@123"),
            () -> time.set(expiry(f)), com.jeepclub.backend.iam.authentication.core.application.exceptions.login.PasswordChangeChallengeInvalidException.class);
        assertThat(accounts.findById(f.id()).orElseThrow().getCredentialStatus()).isEqualTo(
            com.jeepclub.backend.iam.authentication.core.domain.enums.CredentialStatus.PENDING_FIRST_ACCESS);
    }
    private Future<?> pendingResult;
    private void withBlockedAccount(Fixture f, Callable<?> operation, Runnable beforeRelease, Class<? extends Throwable> failure) throws Exception {
        withBlockedRow(f, "select identity_id from authentication_accounts where identity_id=? for update", operation, beforeRelease, failure);
    }
    private void withBlockedRow(Fixture f, String sql, Callable<?> operation, Runnable beforeRelease, Class<? extends Throwable> failure) throws Exception {
        var executor = Executors.newSingleThreadExecutor();
        try (var lock = DriverManager.getConnection(DB.getJdbcUrl(), DB.getUsername(), DB.getPassword())) {
            lock.setAutoCommit(false);
            try (var s = lock.prepareStatement(sql)) {
                s.setLong(1, f.id()); try (var row = s.executeQuery()) { assertThat(row.next()).isTrue(); }
            }
            Future<?> pending = executor.submit(operation);
            try { awaitLock(); beforeRelease.run(); } finally { lock.rollback(); }
            assertThatThrownBy(() -> pending.get(15, TimeUnit.SECONDS)).hasCauseInstanceOf(failure);
        } finally { executor.shutdownNow(); }
    }
    private void awaitLock() {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (jdbc.queryForObject("select count(*) from performance_schema.data_lock_waits", Long.class) > 0) {
                System.out.println("BACK-597 observed MySQL data_lock_waits before clock advance/reissue"); return;
            }
            Thread.yield();
        }
        throw new AssertionError("No real MySQL lock contention observed");
    }
}
