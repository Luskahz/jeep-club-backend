package com.jeepclub.backend.authentication.core.domain.model;

import com.jeepclub.backend.iam.authentication.core.domain.enums.*;
import com.jeepclub.backend.iam.authentication.core.domain.exception.account.*;
import com.jeepclub.backend.iam.authentication.core.domain.exception.passwordrecovery.*;
import com.jeepclub.backend.iam.authentication.core.domain.exception.refreshtoken.RefreshTokenValidationException;
import com.jeepclub.backend.iam.authentication.core.domain.model.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.time.*;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

/** Gaps identified by PIT: credential guards and invalid persisted lifecycle states. */
class AuthenticationBoundaryInvariantTest {
    private static final Instant NOW = Instant.parse("2026-10-15T12:00:00Z");

    @ParameterizedTest
    @NullSource
    @ValueSource(longs = {0, -1})
    void accountCreationAndReconstitutionRejectInvalidIdentity(Long id) {
        assertThatThrownBy(() -> AuthenticationAccount.create(id, "hash", NOW)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> restored(id, "hash", AuthenticationAccessStatus.ENABLED,
                AuthenticationStatus.ENABLED, null, NOW, NOW, 0)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void everyCredentialEntryPointRejectsMissingHashWithoutChangingAccount(String hash) {
        AuthenticationAccount account = account();
        assertThatThrownBy(() -> AuthenticationAccount.create(1L, hash, NOW))
                .isInstanceOf(AuthenticationAccountHashRequiredException.class);
        assertThatThrownBy(() -> restored(1L, hash, AuthenticationAccessStatus.ENABLED,
                AuthenticationStatus.ENABLED, null, NOW, NOW, 0))
                .isInstanceOf(AuthenticationAccountHashRequiredException.class);
        assertThatThrownBy(() -> account.changePassword(hash, NOW))
                .isInstanceOf(AuthenticationAccountHashRequiredException.class);
        assertThatThrownBy(() -> account.changeToTemporaryPassword(hash, NOW))
                .isInstanceOf(AuthenticationAccountHashRequiredException.class);
        assertThat(account.getPasswordHash()).isEqualTo("hash");
        assertThat(account.getCredentialStatus()).isEqualTo(CredentialStatus.PERMANENT);
    }

    @Test
    void blockedAccountCannotAuthenticateOrRequestPasswordChange() {
        AuthenticationAccount account = account();
        account.disableAccess(NOW);
        assertThatThrownBy(account::assertCanAuthenticate).isInstanceOf(AuthenticationAccountBlockedException.class);
        assertThatThrownBy(account::assertCanRequestPasswordChange)
                .isInstanceOf(AuthenticationAccountCannotChangePasswordException.class);
    }

    @Test
    void credentialPredicatesDistinguishPermanentTemporaryAndFirstAccess() {
        AuthenticationAccount account = account();
        assertThat(account.isChangePasswordRequired()).isFalse();
        assertThat(account.isPendingFirstAccess()).isFalse();
        account.changeToTemporaryPassword("temporary-hash", NOW);
        assertThat(account.isChangePasswordRequired()).isTrue();
        assertThat(account.isPendingFirstAccess()).isFalse();
        assertThat(account.isAuthenticationAllowed()).isFalse();
        account.changePassword("permanent-hash", NOW);
        assertThat(account.isChangePasswordRequired()).isFalse();
        assertThat(account.isAuthenticationAllowed()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"password", "temporary", "disable", "enable", "login"})
    void mutationBeforeAccountCreationIsRejectedWithoutChangingFields(String operation) {
        AuthenticationAccount account = account();
        if (operation.equals("enable")) account.disableAccess(NOW);
        var beforeStatus = account.getAccessStatus();
        assertThatThrownBy(() -> mutate(account, operation, NOW.minusSeconds(1)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(account.getPasswordHash()).isEqualTo("hash");
        assertThat(account.getAccessStatus()).isEqualTo(beforeStatus);
        assertThat(account.getLastLoginAt()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"disabled", "updated", "password"})
    void reconstitutionRejectsTimestampsBeforeCreation(String field) {
        assertThatThrownBy(() -> restored(1L, "hash",
                field.equals("disabled") ? AuthenticationAccessStatus.DISABLED : AuthenticationAccessStatus.ENABLED,
                AuthenticationStatus.ENABLED,
                field.equals("disabled") ? NOW.minusSeconds(1) : null,
                field.equals("updated") ? NOW.minusSeconds(1) : NOW,
                field.equals("password") ? NOW.minusSeconds(1) : NOW, 0))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("before createdAt");
    }

    @Test
    void persistedLockRequiresFiveAttemptsButEnabledAccountMayHaveZero() {
        assertThat(restored(1L, "hash", AuthenticationAccessStatus.ENABLED,
                AuthenticationStatus.ENABLED, null, NOW, NOW, 0).isLocked()).isFalse();
        assertThatThrownBy(() -> restored(1L, "hash", AuthenticationAccessStatus.ENABLED,
                AuthenticationStatus.LOCKED, null, NOW, NOW, 4)).isInstanceOf(IllegalArgumentException.class);
        assertThat(restored(1L, "hash", AuthenticationAccessStatus.ENABLED,
                AuthenticationStatus.LOCKED, null, NOW, NOW, 5).isLocked()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"RESET,false", "TEMPORARY,false", "RESET,true", "TEMPORARY,true"})
    void administrativeRecoveryMethodCannotChangeAfterResolutionOrExpiration(String method, boolean expired) {
        var recovery = PasswordRecoveryRequest.createOpenRequest(1L, NOW, NOW.plusSeconds(60));
        recovery.changeToEmailTokenMethod("hash", NOW);
        if (!expired) recovery.resolve(NOW.plusSeconds(1));
        Instant when = expired ? NOW.plusSeconds(61) : NOW.plusSeconds(2);
        assertThatThrownBy(() -> {
            if (method.equals("RESET")) recovery.changeToAdminResetLinkMethod("replacement", when);
            else recovery.changeToAdminTemporaryPasswordMethod(when);
        }).isInstanceOf(PasswordRecoveryRequestStateException.class);
        assertThat(recovery.getMethod()).isEqualTo(PasswordRecoveryRequestMethod.EMAIL_TOKEN);
    }

    static Stream<Arguments> invalidRecoveryStates() {
        return Stream.of(
                Arguments.of(PasswordRecoveryRequestStatus.RESOLVED, null, null),
                Arguments.of(PasswordRecoveryRequestStatus.RESOLVED, NOW.minusSeconds(1), null),
                Arguments.of(PasswordRecoveryRequestStatus.RESOLVED, NOW.plusSeconds(60), null),
                Arguments.of(PasswordRecoveryRequestStatus.OPEN, NOW, null),
                Arguments.of(PasswordRecoveryRequestStatus.CANCELLED, null, null),
                Arguments.of(PasswordRecoveryRequestStatus.CANCELLED, null, NOW.minusSeconds(1)),
                Arguments.of(PasswordRecoveryRequestStatus.OPEN, null, NOW));
    }

    @ParameterizedTest
    @MethodSource("invalidRecoveryStates")
    void reconstitutionRejectsContradictoryRecoveryLifecycle(
            PasswordRecoveryRequestStatus status, Instant resolved, Instant cancelled) {
        assertThatThrownBy(() -> PasswordRecoveryRequest.reconstitute(1L, 1L, "hash",
                NOW.plusSeconds(60), NOW, resolved, cancelled, status, PasswordRecoveryRequestMethod.EMAIL_TOKEN))
                .isInstanceOf(PasswordRecoveryRequestValidationException.class);
    }

    @Test
    void cancelledRecoveryRestoresAuditTimestampAndRejectsZeroIdentifier() {
        var cancelled = PasswordRecoveryRequest.reconstitute(1L, 1L, "hash", NOW.plusSeconds(60),
                NOW, null, NOW.plusSeconds(1), PasswordRecoveryRequestStatus.CANCELLED,
                PasswordRecoveryRequestMethod.EMAIL_TOKEN);
        assertThat(cancelled.getCancelledAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(cancelled.getStatus()).isEqualTo(PasswordRecoveryRequestStatus.CANCELLED);
        assertThatThrownBy(() -> PasswordRecoveryRequest.reconstitute(0L, 1L, null, NOW.plusSeconds(60),
                NOW, null, null, PasswordRecoveryRequestStatus.OPEN, PasswordRecoveryRequestMethod.UNDEFINED))
                .isInstanceOf(PasswordRecoveryRequestValidationException.class);
    }

    @Test
    void maximumHashLengthsAreAcceptedAndZeroReplacementIdsAreRejected() {
        var recovery = PasswordRecoveryRequest.createOpenRequest(1L, NOW, NOW.plusSeconds(60));
        recovery.changeToEmailTokenMethod("a".repeat(128), NOW);
        assertThat(recovery.getTokenHash()).hasSize(128);
        assertThatThrownBy(() -> recovery.changeToEmailTokenMethod("a".repeat(129), NOW))
                .isInstanceOf(PasswordRecoveryRequestValidationException.class);
        Session session = Session.reconstitute(1L, 1L, NOW, NOW.plusSeconds(120), null, SessionStatus.ACTIVE);
        var token = RefreshToken.create(session, "a".repeat(255), Duration.ofSeconds(60), NOW);
        assertThat(token.getTokenHash()).hasSize(255);
        assertThatThrownBy(() -> token.rotate(0L, NOW.plusSeconds(1)))
                .isInstanceOf(RefreshTokenValidationException.class);
        assertThatThrownBy(() -> RefreshToken.reconstitute(1L, session, "hash", NOW, NOW.plusSeconds(60),
                RefreshTokenStatus.ROTATED, 0L)).isInstanceOf(RefreshTokenValidationException.class);
    }

    private AuthenticationAccount account() { return AuthenticationAccount.create(1L, "hash", NOW); }

    private AuthenticationAccount restored(Long id, String hash, AuthenticationAccessStatus access,
            AuthenticationStatus authentication, Instant disabled, Instant updated, Instant password, int attempts) {
        return AuthenticationAccount.reconstitute(id, hash, access, authentication, CredentialStatus.PERMANENT,
                null, NOW, disabled, updated, password, attempts);
    }

    private void mutate(AuthenticationAccount account, String operation, Instant when) {
        switch (operation) {
            case "password" -> account.changePassword("changed", when);
            case "temporary" -> account.changeToTemporaryPassword("changed", when);
            case "disable" -> account.disableAccess(when);
            case "enable" -> account.enableAccess(when);
            case "login" -> account.recordSuccessfulLogin(when);
            default -> throw new AssertionError(operation);
        }
    }
}
