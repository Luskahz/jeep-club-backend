package com.jeepclub.backend.authentication.core.application.service;
import com.jeepclub.backend.iam.authentication.core.application.service.session.SessionService;
import com.jeepclub.backend.iam.authentication.core.application.service.passwordrecovery.AdminPasswordRecoveryService;
import com.jeepclub.backend.iam.authentication.core.application.exceptions.login.*;
import com.jeepclub.backend.iam.authentication.core.application.result.login.PasswordChangeRequiredLoginResult;
import com.jeepclub.backend.iam.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.iam.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.iam.authentication.infra.persistence.jpa.AuthenticationAccountJpaRepository;
import com.jeepclub.backend.iam.identity.api.module.*;
import com.jeepclub.backend.memberships.api.module.MembershipOnboardingCompletion;
import java.time.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ManualPasswordExpirationIntegrationTest {
    protected static final Instant START = Instant.parse("2026-11-08T12:00:00Z");
    private static final AtomicReference<Instant> TIME = new AtomicReference<>(START);
    protected final AtomicReference<Instant> time = TIME;
    @org.springframework.test.context.bean.override.convention.TestBean(methodName = "testClock") protected Clock clock;
    static Clock testClock() {
        return new Clock() {
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId zone) { return this; }
            public Instant instant() { return TIME.get(); }
        };
    }
    @Autowired protected UserRegistration registration;
    @Autowired protected UserQuery users;
    @Autowired protected SessionService sessions;
    @Autowired protected AdminPasswordRecoveryService recovery;
    @Autowired protected AuthenticationAccountJpaRepository accounts;
    @Autowired protected AuthenticationAccountRepository domainAccounts;
    @Autowired protected com.jeepclub.backend.iam.authentication.core.repository.PasswordRecoveryRequestRepository requests;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected TransactionTemplate transaction;
    @MockitoSpyBean protected MembershipOnboardingCompletion onboarding;
    private static final java.util.concurrent.atomic.AtomicInteger IDS = new java.util.concurrent.atomic.AtomicInteger(100000000);
    protected record Fixture(long id, String cpf, String password) {}
    @BeforeEach void clock() { time.set(START); }
    protected Fixture manual() {
        String cpf = cpf();
        var result = registration.createAdministratively(data(cpf), UserActivationMethod.TEMPORARY_PASSWORD, 1L);
        return new Fixture(result.userId(), cpf, result.temporaryPassword());
    }
    protected UserRegistrationData data(String cpf) {
        return new UserRegistrationData("Manual expiry", null, cpf + "@example.com", cpf, null, null, null, time.get());
    }
    protected String cpf() {
        String digits = Integer.toString(IDS.incrementAndGet());
        for (int length = 9; length < 11; length++) {
            int sum = 0;
            for (int i = 0; i < length; i++) sum += (digits.charAt(i) - '0') * (length + 1 - i);
            int check = 11 - sum % 11;
            digits += check >= 10 ? 0 : check;
        }
        return digits;
    }
    protected PasswordChangeRequiredLoginResult challenge(Fixture f) {
        return (PasswordChangeRequiredLoginResult) sessions.login(f.cpf(), f.password());
    }
    protected Instant expiry(Fixture f) { return accounts.findById(f.id()).orElseThrow().getManualPasswordExpiresAt(); }
    @Test void administrativeCreationPersistsExactly24HoursAndRejectsAtAndAfterDeadline() {
        Fixture f = manual(); assertThat(f.password()).matches("[a-z]{5}[0-9]{3}");
        assertThat(expiry(f)).isEqualTo(START.plusSeconds(86400));
        time.set(expiry(f).minusNanos(1000)); challenge(f);
        time.set(expiry(f)); assertThatThrownBy(() -> challenge(f)).isInstanceOf(InvalidCredentialsException.class);
        time.set(expiry(f).plusSeconds(1)); assertThatThrownBy(() -> challenge(f)).isInstanceOf(InvalidCredentialsException.class);
    }
    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(longs = {0, 1})
    void challengeIssuedBeforeExpiryCannotCompleteAtOrAfterDeadlineAndRollsBack(long secondsAfter) {
        Fixture f = manual(); time.set(expiry(f).minusSeconds(60));
        var c = challenge(f); String hash = accounts.findById(f.id()).orElseThrow().getPasswordHash();
        time.set(expiry(f).plusSeconds(secondsAfter));
        assertThatThrownBy(() -> sessions.completeRequiredPasswordChange(c.passwordChangeToken(), "NewPassword@123"))
                .isInstanceOf(PasswordChangeChallengeInvalidException.class);
        assertThat(accounts.findById(f.id()).orElseThrow().getPasswordHash()).isEqualTo(hash);
        assertThat(accounts.findById(f.id()).orElseThrow().getCredentialStatus()).isEqualTo(CredentialStatus.PENDING_FIRST_ACCESS);
    }
    @Test void reissueRenews24HoursInvalidatesPreviousPasswordAndChallengeAndCompletesFirstAccess() {
        Fixture f = manual(); time.set(expiry(f).minusSeconds(60)); var oldChallenge = challenge(f);
        String replacement = recovery.generateTemporaryPassword(f.id()).temporaryPassword();
        assertThat(expiry(f)).isEqualTo(time.get().plusSeconds(86400));
        assertThat(accounts.findById(f.id()).orElseThrow().getCredentialStatus()).isEqualTo(CredentialStatus.PENDING_FIRST_ACCESS);
        assertThatThrownBy(() -> sessions.login(f.cpf(), f.password())).isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> sessions.completeRequiredPasswordChange(oldChallenge.passwordChangeToken(), "NewPassword@123"))
                .isInstanceOf(PasswordChangeChallengeInvalidException.class);
        var c = challenge(new Fixture(f.id(), f.cpf(), replacement));
        sessions.completeRequiredPasswordChange(c.passwordChangeToken(), "NewPassword@123");
        assertThat(expiry(f)).isNull();
        verify(onboarding).completeApprovedApplicationForIdentity(f.id(), time.get());
        assertThat(requests.findByUserId(f.id())).allSatisfy(r -> assertThat(r.isOpen(time.get())).isFalse());
        assertThatThrownBy(() -> sessions.completeRequiredPasswordChange(c.passwordChangeToken(), "NewPassword@123"))
                .isInstanceOf(PasswordChangeChallengeInvalidException.class);
    }
    @Test void fiveFailuresAndUnlockDoNotRenewExpiredCredential() {
        Fixture f = manual();
        for (int i = 0; i < 5; i++) assertThatThrownBy(() -> sessions.login(f.cpf(), "wrong")).isInstanceOf(InvalidCredentialsException.class);
        assertThat(accounts.findById(f.id()).orElseThrow().getFailedLoginAttempts()).isEqualTo(5);
        time.set(expiry(f));
        transaction.executeWithoutResult(tx -> { var account = domainAccounts.findByIdentityIdForUpdate(f.id()).orElseThrow(); account.unlock(); domainAccounts.save(account); });
        assertThatThrownBy(() -> challenge(f)).isInstanceOf(InvalidCredentialsException.class);
        assertThat(expiry(f)).isEqualTo(START.plusSeconds(86400));
    }
    @Test void recoveryRevokesSessionsAndRefreshTokensAndDoesNotInheritOldRequestExpiry() {
        String cpf = cpf(); registration.registerAndAuthenticate(data(cpf), "OldPassword@123");
        long id = users.findByCpf(cpf).orElseThrow().id();
        recovery.generateTemporaryPassword(id);
        assertThat(jdbc.queryForObject("select count(*) from authentication_sessions where user_id=? and session_status='ACTIVE'", Long.class, id)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from authentication_refresh_tokens t join authentication_sessions s on s.id=t.session_id where s.user_id=? and t.status='ACTIVE'", Long.class, id)).isZero();
        time.set(START.plusSeconds(86390));
        String raw = recovery.generateTemporaryPassword(id).temporaryPassword();
        Fixture f = new Fixture(id, cpf, raw);
        assertThat(expiry(f)).isEqualTo(time.get().plusSeconds(86400));
        assertThat(requests.findByUserId(id).stream().filter(r -> r.isOpen(time.get())).findFirst().orElseThrow().getExpiresAt()).isEqualTo(expiry(f));
        var c = challenge(f); sessions.completeRequiredPasswordChange(c.passwordChangeToken(), "NewPassword@123");
        assertThat(expiry(f)).isNull();
    }
    @Test void legacyCredentialRemainsUsableWithoutManualExpiry() {
        String cpf = cpf(); long id = registration.createPendingFirstAccess(data(cpf), "legacy-strong");
        time.set(START.plusSeconds(172800));
        assertThat(accounts.findById(id).orElseThrow().getManualPasswordExpiresAt()).isNull();
        challenge(new Fixture(id, cpf, "legacy-strong"));
    }
    @Test void membershipManualProvisioningKeepsPendingFirstAccessAndCompletesOnboarding() {
        String cpf = cpf(); long id = registration.createPendingManualFirstAccess(data(cpf), "abcde012");
        Fixture f = new Fixture(id, cpf, "abcde012");
        assertThat(expiry(f)).isEqualTo(START.plusSeconds(86400));
        var c = challenge(f); sessions.completeRequiredPasswordChange(c.passwordChangeToken(), "NewPassword@123");
        verify(onboarding).completeApprovedApplicationForIdentity(id, START);
        assertThat(expiry(f)).isNull();
    }
    @Test void failedOnboardingRollsBackPasswordExpiryChallengeAndTokens() {
        Fixture f = manual(); var c = challenge(f);
        doThrow(new IllegalStateException("synthetic rollback")).when(onboarding).completeApprovedApplicationForIdentity(f.id(), START);
        assertThatThrownBy(() -> sessions.completeRequiredPasswordChange(c.passwordChangeToken(), "NewPassword@123")).isInstanceOf(IllegalStateException.class);
        assertThat(expiry(f)).isEqualTo(START.plusSeconds(86400));
        doNothing().when(onboarding).completeApprovedApplicationForIdentity(f.id(), START);
        sessions.completeRequiredPasswordChange(c.passwordChangeToken(), "NewPassword@123");
        assertThat(expiry(f)).isNull();
    }
}
