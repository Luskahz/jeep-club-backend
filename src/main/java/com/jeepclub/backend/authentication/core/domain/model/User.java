package com.jeepclub.backend.authentication.core.domain.model;

import com.jeepclub.backend.authentication.core.domain.enums.AccountStatus;
import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationStatus;
import com.jeepclub.backend.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.authentication.core.domain.enums.UserStatus;
import com.jeepclub.backend.authentication.core.domain.exception.user.UserAlreadyDisabledException;
import com.jeepclub.backend.authentication.core.domain.exception.user.UserBlockedForLoginException;
import com.jeepclub.backend.authentication.core.domain.exception.user.UserCannotChangePasswordException;
import com.jeepclub.backend.authentication.core.domain.exception.user.UserNewHashRequiredException;
import com.jeepclub.backend.authentication.core.domain.exception.user.UserNotDisabledException;
import com.jeepclub.backend.authentication.core.domain.exception.user.UserNotLockoutException;
import com.jeepclub.backend.authentication.core.domain.exception.user.UserNowInstantRequiredException;
import com.jeepclub.backend.authentication.core.domain.exception.user.UserPasswordChangeRequiredException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class User {

    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;

    private Long id;
    private String name;
    private LocalDate birthDate;
    private String email;
    private String cpf;
    private String rg;
    private String passwordHash;
    private String phoneNumber;
    private String profilePhotoUrl;
    private AccountStatus accountStatus;
    private AuthenticationStatus authenticationStatus;
    private CredentialStatus credentialStatus;
    private Instant lastLoginAt;
    private Instant createdAt;
    private Instant disabledAt;
    private Instant updatedAt;
    private Instant passwordChangedAt;
    private int failedLoginAttempts;

    public static User create(
            String name,
            LocalDate birthDate,
            String email,
            String cpf,
            String rg,
            String passwordHash,
            String phoneNumber,
            Instant now
    ) {
        return createWithCredentialStatus(
                name,
                birthDate,
                email,
                cpf,
                rg,
                passwordHash,
                phoneNumber,
                CredentialStatus.PERMANENT,
                now
        );
    }

    public static User createPendingFirstAccess(
            String name,
            String email,
            String cpf,
            String passwordHash,
            String phoneNumber,
            Instant now
    ) {
        return createWithCredentialStatus(
                name,
                null,
                email,
                cpf,
                null,
                passwordHash,
                phoneNumber,
                CredentialStatus.PENDING_FIRST_ACCESS,
                now
        );
    }

    private static User createWithCredentialStatus(
            String name,
            LocalDate birthDate,
            String email,
            String cpf,
            String rg,
            String passwordHash,
            String phoneNumber,
            CredentialStatus credentialStatus,
            Instant now
    ) {
        requireText(name, "name");
        requireText(cpf, "cpf");
        requireText(passwordHash, "passwordHash");
        Objects.requireNonNull(credentialStatus, "credentialStatus cannot be null");
        validateNow(now);

        User user = new User();
        user.name = name;
        user.birthDate = birthDate;
        user.email = email;
        user.cpf = cpf;
        user.rg = rg;
        user.passwordHash = passwordHash;
        user.phoneNumber = phoneNumber;
        user.accountStatus = AccountStatus.ACTIVE;
        user.authenticationStatus = AuthenticationStatus.ENABLED;
        user.credentialStatus = credentialStatus;
        user.createdAt = now;
        return user;
    }

    public static User reconstitute(
            Long id,
            String name,
            LocalDate birthDate,
            String email,
            String cpf,
            String rg,
            String passwordHash,
            String phoneNumber,
            String profilePhotoUrl,
            AccountStatus accountStatus,
            AuthenticationStatus authenticationStatus,
            CredentialStatus credentialStatus,
            Instant lastLoginAt,
            Instant createdAt,
            Instant disabledAt,
            Instant updatedAt,
            Instant passwordChangedAt,
            int failedLoginAttempts
    ) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        requireText(name, "name");
        requireText(cpf, "cpf");
        requireText(passwordHash, "passwordHash");
        Objects.requireNonNull(accountStatus, "accountStatus cannot be null");
        Objects.requireNonNull(authenticationStatus, "authenticationStatus cannot be null");
        Objects.requireNonNull(credentialStatus, "credentialStatus cannot be null");
        Objects.requireNonNull(createdAt, "createdAt cannot be null");
        if (failedLoginAttempts < 0) {
            throw new IllegalArgumentException("failedLoginAttempts cannot be negative");
        }
        if (accountStatus == AccountStatus.DISABLED && disabledAt == null) {
            throw new IllegalArgumentException("disabled account must have disabledAt");
        }
        if (accountStatus == AccountStatus.ACTIVE && disabledAt != null) {
            throw new IllegalArgumentException("active account cannot have disabledAt");
        }
        if (authenticationStatus == AuthenticationStatus.LOCKED
                && failedLoginAttempts < MAX_FAILED_LOGIN_ATTEMPTS) {
            throw new IllegalArgumentException("locked user must have reached the login attempt limit");
        }

        User user = new User();
        user.id = id;
        user.name = name;
        user.birthDate = birthDate;
        user.email = email;
        user.cpf = cpf;
        user.rg = rg;
        user.passwordHash = passwordHash;
        user.phoneNumber = phoneNumber;
        user.profilePhotoUrl = profilePhotoUrl;
        user.accountStatus = accountStatus;
        user.authenticationStatus = authenticationStatus;
        user.credentialStatus = credentialStatus;
        user.lastLoginAt = lastLoginAt;
        user.createdAt = createdAt;
        user.disabledAt = disabledAt;
        user.updatedAt = updatedAt;
        user.passwordChangedAt = passwordChangedAt;
        user.failedLoginAttempts = failedLoginAttempts;
        return user;
    }

    /**
     * Compatibility projection priority: disabled, locked, required change,
     * pending first access, active.
     */
    public UserStatus getStatus() {
        if (isDisabled()) {
            return UserStatus.DISABLED;
        }
        if (isLocked()) {
            return UserStatus.LOCKED;
        }
        if (credentialStatus == CredentialStatus.CHANGE_REQUIRED) {
            return UserStatus.CHANGE_PASSWORD_REQUIRED;
        }
        if (credentialStatus == CredentialStatus.PENDING_FIRST_ACCESS) {
            return UserStatus.PENDING_FIRST_ACCESS;
        }
        return UserStatus.ACTIVE;
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
        if (isDisabled() || isLocked()) {
            throw new UserBlockedForLoginException("User cannot authenticate.");
        }
    }

    public void assertCanAuthenticate() {
        assertCanAttemptLogin();
        if (credentialStatus != CredentialStatus.PERMANENT) {
            throw new UserPasswordChangeRequiredException("Credential is not permanent.");
        }
    }

    public boolean isBlockedForLogin() {
        return isLocked() || isDisabled();
    }

    public boolean isActive() {
        return accountStatus == AccountStatus.ACTIVE
                && authenticationStatus == AuthenticationStatus.ENABLED
                && credentialStatus == CredentialStatus.PERMANENT;
    }

    public boolean isLocked() {
        return authenticationStatus == AuthenticationStatus.LOCKED;
    }

    public boolean isDisabled() {
        return accountStatus == AccountStatus.DISABLED;
    }

    public boolean isChangePasswordRequired() {
        return credentialStatus == CredentialStatus.CHANGE_REQUIRED;
    }

    public void unlock() {
        if (!isLocked()) {
            throw new UserNotLockoutException("User is not locked.");
        }
        authenticationStatus = AuthenticationStatus.ENABLED;
        failedLoginAttempts = 0;
    }

    public void recordSuccessfulLogin(Instant now) {
        validateNow(now);
        lastLoginAt = now;
        failedLoginAttempts = 0;
        authenticationStatus = AuthenticationStatus.ENABLED;
        updatedAt = now;
    }

    public void changePassword(String newHash, Instant now) {
        requireHash(newHash);
        validateNow(now);
        passwordHash = newHash;
        passwordChangedAt = now;
        failedLoginAttempts = 0;
        authenticationStatus = AuthenticationStatus.ENABLED;
        credentialStatus = CredentialStatus.PERMANENT;
        updatedAt = now;
    }

    public void changeToTemporaryPassword(String temporaryPasswordHash, Instant now) {
        requireHash(temporaryPasswordHash);
        validateNow(now);
        passwordHash = temporaryPasswordHash;
        passwordChangedAt = now;
        failedLoginAttempts = 0;
        authenticationStatus = AuthenticationStatus.ENABLED;
        credentialStatus = CredentialStatus.CHANGE_REQUIRED;
        updatedAt = now;
    }

    public void assertCanRequestPasswordChange() {
        if (isDisabled()) {
            throw new UserCannotChangePasswordException("User cannot request password change.");
        }
    }

    public void disable(Instant now) {
        validateNow(now);
        if (isDisabled()) {
            throw new UserAlreadyDisabledException(id);
        }
        accountStatus = AccountStatus.DISABLED;
        disabledAt = now;
        updatedAt = now;
    }

    public void enable(Instant now) {
        validateNow(now);
        if (!isDisabled()) {
            throw new UserNotDisabledException(id);
        }
        accountStatus = AccountStatus.ACTIVE;
        disabledAt = null;
        updatedAt = now;
    }

    private static void requireHash(String hash) {
        if (hash == null || hash.isBlank()) {
            throw new UserNewHashRequiredException("password hash is required.");
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }

    private static void validateNow(Instant now) {
        if (now == null) {
            throw new UserNowInstantRequiredException("now is required.");
        }
    }
}
