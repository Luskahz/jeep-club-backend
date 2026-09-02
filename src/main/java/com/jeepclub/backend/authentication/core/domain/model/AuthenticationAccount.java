package com.jeepclub.backend.authentication.core.domain.model;

import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationAccessStatus;
import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationStatus;
import com.jeepclub.backend.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.authentication.core.domain.exception.account.AuthenticationAccountAlreadyDisabledException;
import com.jeepclub.backend.authentication.core.domain.exception.account.AuthenticationAccountBlockedException;
import com.jeepclub.backend.authentication.core.domain.exception.account.AuthenticationAccountCannotChangePasswordException;
import com.jeepclub.backend.authentication.core.domain.exception.account.AuthenticationAccountHashRequiredException;
import com.jeepclub.backend.authentication.core.domain.exception.account.AuthenticationAccountNotDisabledException;
import com.jeepclub.backend.authentication.core.domain.exception.account.AuthenticationAccountNotLockedException;
import com.jeepclub.backend.authentication.core.domain.exception.account.AuthenticationAccountPasswordChangeRequiredException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthenticationAccount {

    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;

    private Long identityId;
    private String passwordHash;
    private AuthenticationAccessStatus accessStatus;
    private AuthenticationStatus authenticationStatus;
    private CredentialStatus credentialStatus;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant accessDisabledAt;
    private Instant updatedAt;
    private Instant passwordChangedAt;
    private int failedLoginAttempts;

    public static AuthenticationAccount create(
            Long identityId,
            String passwordHash,
            Instant now
    ) {
        return createWithCredentialStatus(
                identityId,
                passwordHash,
                CredentialStatus.PERMANENT,
                now
        );
    }

    public static AuthenticationAccount createPendingFirstAccess(
            Long identityId,
            String passwordHash,
            Instant now
    ) {
        return createWithCredentialStatus(
                identityId,
                passwordHash,
                CredentialStatus.PENDING_FIRST_ACCESS,
                now
        );
    }

    private static AuthenticationAccount createWithCredentialStatus(
            Long identityId,
            String passwordHash,
            CredentialStatus credentialStatus,
            Instant now
    ) {
        validateIdentityId(identityId);
        requireHash(passwordHash);
        Objects.requireNonNull(credentialStatus, "credentialStatus cannot be null");
        Objects.requireNonNull(now, "now cannot be null");

        AuthenticationAccount account = new AuthenticationAccount();
        account.identityId = identityId;
        account.passwordHash = passwordHash;
        account.accessStatus = AuthenticationAccessStatus.ENABLED;
        account.authenticationStatus = AuthenticationStatus.ENABLED;
        account.credentialStatus = credentialStatus;
        account.createdAt = now;
        return account;
    }

    public static AuthenticationAccount reconstitute(
            Long identityId,
            String passwordHash,
            AuthenticationAccessStatus accessStatus,
            AuthenticationStatus authenticationStatus,
            CredentialStatus credentialStatus,
            Instant lastLoginAt,
            Instant createdAt,
            Instant accessDisabledAt,
            Instant updatedAt,
            Instant passwordChangedAt,
            int failedLoginAttempts
    ) {
        validateIdentityId(identityId);
        requireHash(passwordHash);
        Objects.requireNonNull(accessStatus, "accessStatus cannot be null");
        Objects.requireNonNull(authenticationStatus, "authenticationStatus cannot be null");
        Objects.requireNonNull(credentialStatus, "credentialStatus cannot be null");
        Objects.requireNonNull(createdAt, "createdAt cannot be null");
        validateReconstitutedState(
                accessStatus,
                authenticationStatus,
                createdAt,
                accessDisabledAt,
                updatedAt,
                passwordChangedAt,
                failedLoginAttempts
        );

        AuthenticationAccount account = new AuthenticationAccount();
        account.identityId = identityId;
        account.passwordHash = passwordHash;
        account.accessStatus = accessStatus;
        account.authenticationStatus = authenticationStatus;
        account.credentialStatus = credentialStatus;
        account.lastLoginAt = lastLoginAt;
        account.createdAt = createdAt;
        account.accessDisabledAt = accessDisabledAt;
        account.updatedAt = updatedAt;
        account.passwordChangedAt = passwordChangedAt;
        account.failedLoginAttempts = failedLoginAttempts;
        return account;
    }

    public void registerFailedLogin() {
        if (isBlockedForLogin()) {
            return;
        }

        failedLoginAttempts++;
        if (failedLoginAttempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
            authenticationStatus = AuthenticationStatus.LOCKED;
        }
    }

    public void assertCanAttemptLogin() {
        if (isBlockedForLogin()) {
            throw new AuthenticationAccountBlockedException();
        }
    }

    public void assertCanAuthenticate() {
        assertCanAttemptLogin();
        if (credentialStatus != CredentialStatus.PERMANENT) {
            throw new AuthenticationAccountPasswordChangeRequiredException();
        }
    }

    public void assertCanRequestPasswordChange() {
        if (isAccessDisabled()) {
            throw new AuthenticationAccountCannotChangePasswordException();
        }
    }

    public boolean isBlockedForLogin() {
        return isAccessDisabled() || isLocked();
    }

    public boolean isAuthenticationAllowed() {
        return !isBlockedForLogin()
                && credentialStatus == CredentialStatus.PERMANENT;
    }

    public boolean isLocked() {
        return authenticationStatus == AuthenticationStatus.LOCKED;
    }

    public boolean isAccessDisabled() {
        return accessStatus == AuthenticationAccessStatus.DISABLED;
    }

    public boolean isChangePasswordRequired() {
        return credentialStatus == CredentialStatus.CHANGE_REQUIRED
                || credentialStatus == CredentialStatus.PENDING_FIRST_ACCESS;
    }

    public boolean isPendingFirstAccess() {
        return credentialStatus == CredentialStatus.PENDING_FIRST_ACCESS;
    }

    public void unlock() {
        if (!isLocked()) {
            throw new AuthenticationAccountNotLockedException();
        }

        authenticationStatus = AuthenticationStatus.ENABLED;
        failedLoginAttempts = 0;
    }

    public void recordSuccessfulLogin(Instant now) {
        validateMutationInstant(now);
        lastLoginAt = now;
        failedLoginAttempts = 0;
        authenticationStatus = AuthenticationStatus.ENABLED;
        updatedAt = now;
    }

    public void changePassword(String newHash, Instant now) {
        requireHash(newHash);
        validateMutationInstant(now);
        passwordHash = newHash;
        passwordChangedAt = now;
        failedLoginAttempts = 0;
        authenticationStatus = AuthenticationStatus.ENABLED;
        credentialStatus = CredentialStatus.PERMANENT;
        updatedAt = now;
    }

    public void changeToTemporaryPassword(String temporaryPasswordHash, Instant now) {
        requireHash(temporaryPasswordHash);
        validateMutationInstant(now);
        passwordHash = temporaryPasswordHash;
        passwordChangedAt = now;
        failedLoginAttempts = 0;
        authenticationStatus = AuthenticationStatus.ENABLED;
        credentialStatus = CredentialStatus.CHANGE_REQUIRED;
        updatedAt = now;
    }

    public void disableAccess(Instant now) {
        validateMutationInstant(now);
        if (isAccessDisabled()) {
            throw new AuthenticationAccountAlreadyDisabledException(identityId);
        }

        accessStatus = AuthenticationAccessStatus.DISABLED;
        accessDisabledAt = now;
        updatedAt = now;
    }

    public void enableAccess(Instant now) {
        validateMutationInstant(now);
        if (!isAccessDisabled()) {
            throw new AuthenticationAccountNotDisabledException(identityId);
        }

        accessStatus = AuthenticationAccessStatus.ENABLED;
        accessDisabledAt = null;
        updatedAt = now;
    }

    private void validateMutationInstant(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");
        if (now.isBefore(createdAt)) {
            throw new IllegalArgumentException("now cannot be before createdAt");
        }
    }

    private static void validateIdentityId(Long identityId) {
        if (identityId == null || identityId <= 0) {
            throw new IllegalArgumentException("identityId must be positive");
        }
    }

    private static void requireHash(String hash) {
        if (hash == null || hash.isBlank()) {
            throw new AuthenticationAccountHashRequiredException();
        }
    }

    private static void validateReconstitutedState(
            AuthenticationAccessStatus accessStatus,
            AuthenticationStatus authenticationStatus,
            Instant createdAt,
            Instant accessDisabledAt,
            Instant updatedAt,
            Instant passwordChangedAt,
            int failedLoginAttempts
    ) {
        if (failedLoginAttempts < 0) {
            throw new IllegalArgumentException("failedLoginAttempts cannot be negative");
        }
        if (accessStatus == AuthenticationAccessStatus.DISABLED
                && accessDisabledAt == null) {
            throw new IllegalArgumentException(
                    "disabled authentication access must have accessDisabledAt"
            );
        }
        if (accessStatus == AuthenticationAccessStatus.ENABLED
                && accessDisabledAt != null) {
            throw new IllegalArgumentException(
                    "enabled authentication access cannot have accessDisabledAt"
            );
        }
        if (authenticationStatus == AuthenticationStatus.LOCKED
                && failedLoginAttempts < MAX_FAILED_LOGIN_ATTEMPTS) {
            throw new IllegalArgumentException(
                    "locked authentication account must have reached the login attempt limit"
            );
        }
        validateTimestamp(createdAt, accessDisabledAt, "accessDisabledAt");
        validateTimestamp(createdAt, updatedAt, "updatedAt");
        validateTimestamp(createdAt, passwordChangedAt, "passwordChangedAt");
    }

    private static void validateTimestamp(
            Instant createdAt,
            Instant timestamp,
            String field
    ) {
        if (timestamp != null && timestamp.isBefore(createdAt)) {
            throw new IllegalArgumentException(field + " cannot be before createdAt");
        }
    }
}
