package com.jeepclub.backend.authentication.core.domain.model;

import com.jeepclub.backend.iam.authentication.core.domain.enums.AuthenticationAccessStatus;
import com.jeepclub.backend.iam.authentication.core.domain.enums.AuthenticationStatus;
import com.jeepclub.backend.iam.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.iam.authentication.core.domain.exception.account.AuthenticationAccountBlockedException;
import com.jeepclub.backend.iam.authentication.core.domain.exception.account.AuthenticationAccountPasswordChangeRequiredException;
import com.jeepclub.backend.iam.authentication.core.domain.model.AuthenticationAccount;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthenticationAccountTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void createUsesIdentityIdAsAccountIdAndStartsEnabled() {
        AuthenticationAccount account = AuthenticationAccount.create(
                42L,
                "password-hash",
                CREATED_AT
        );

        assertThat(account.getIdentityId()).isEqualTo(42L);
        assertThat(account.getAccessStatus())
                .isEqualTo(AuthenticationAccessStatus.ENABLED);
        assertThat(account.getAuthenticationStatus())
                .isEqualTo(AuthenticationStatus.ENABLED);
        assertThat(account.getCredentialStatus())
                .isEqualTo(CredentialStatus.PERMANENT);
        assertThat(account.isAuthenticationAllowed()).isTrue();
    }

    @Test
    void pendingFirstAccessIsIndependentFromAdministrativeAccess() {
        AuthenticationAccount account = AuthenticationAccount.createPendingFirstAccess(
                42L,
                "password-hash",
                CREATED_AT
        );

        assertThat(account.getAccessStatus())
                .isEqualTo(AuthenticationAccessStatus.ENABLED);
        assertThat(account.isPendingFirstAccess()).isTrue();
        assertThat(account.isChangePasswordRequired()).isTrue();
        assertThat(account.isAuthenticationAllowed()).isFalse();
        assertThatThrownBy(account::assertCanAuthenticate)
                .isInstanceOf(AuthenticationAccountPasswordChangeRequiredException.class);
    }

    @Test
    void fiveFailedLoginsLockAccountWithoutDisablingAccess() {
        AuthenticationAccount account = AuthenticationAccount.create(
                42L,
                "password-hash",
                CREATED_AT
        );

        for (int attempt = 0; attempt < 5; attempt++) {
            account.registerFailedLogin();
        }

        assertThat(account.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(account.isLocked()).isTrue();
        assertThat(account.getAccessStatus())
                .isEqualTo(AuthenticationAccessStatus.ENABLED);
        assertThatThrownBy(account::assertCanAttemptLogin)
                .isInstanceOf(AuthenticationAccountBlockedException.class);
    }

    @Test
    void disableAndEnableAccessPreserveLockoutAndCredentialState() {
        AuthenticationAccount account = account(
                AuthenticationAccessStatus.ENABLED,
                AuthenticationStatus.LOCKED,
                CredentialStatus.CHANGE_REQUIRED,
                null,
                5
        );
        Instant disabledAt = CREATED_AT.plusSeconds(60);

        account.disableAccess(disabledAt);
        account.enableAccess(CREATED_AT.plusSeconds(120));

        assertThat(account.getAccessStatus())
                .isEqualTo(AuthenticationAccessStatus.ENABLED);
        assertThat(account.getAccessDisabledAt()).isNull();
        assertThat(account.getAuthenticationStatus())
                .isEqualTo(AuthenticationStatus.LOCKED);
        assertThat(account.getCredentialStatus())
                .isEqualTo(CredentialStatus.CHANGE_REQUIRED);
        assertThat(account.getFailedLoginAttempts()).isEqualTo(5);
    }

    @Test
    void unlockDoesNotEnableAdministrativelyDisabledAccess() {
        AuthenticationAccount account = account(
                AuthenticationAccessStatus.DISABLED,
                AuthenticationStatus.LOCKED,
                CredentialStatus.PERMANENT,
                CREATED_AT.plusSeconds(10),
                5
        );

        account.unlock();

        assertThat(account.getAccessStatus())
                .isEqualTo(AuthenticationAccessStatus.DISABLED);
        assertThat(account.getAuthenticationStatus())
                .isEqualTo(AuthenticationStatus.ENABLED);
        assertThat(account.getFailedLoginAttempts()).isZero();
    }

    @Test
    void passwordChangesDoNotEnableAdministrativelyDisabledAccess() {
        AuthenticationAccount account = account(
                AuthenticationAccessStatus.DISABLED,
                AuthenticationStatus.LOCKED,
                CredentialStatus.CHANGE_REQUIRED,
                CREATED_AT.plusSeconds(10),
                5
        );

        account.changePassword("new-password-hash", CREATED_AT.plusSeconds(60));

        assertThat(account.getAccessStatus())
                .isEqualTo(AuthenticationAccessStatus.DISABLED);
        assertThat(account.getCredentialStatus())
                .isEqualTo(CredentialStatus.PERMANENT);
        assertThat(account.getAuthenticationStatus())
                .isEqualTo(AuthenticationStatus.ENABLED);
    }

    @Test
    void reconstitutionRejectsImpossibleAccessTimestampCombination() {
        assertThatIllegalArgumentException().isThrownBy(() -> account(
                AuthenticationAccessStatus.DISABLED,
                AuthenticationStatus.ENABLED,
                CredentialStatus.PERMANENT,
                null,
                0
        ));
    }

    private AuthenticationAccount account(
            AuthenticationAccessStatus accessStatus,
            AuthenticationStatus authenticationStatus,
            CredentialStatus credentialStatus,
            Instant accessDisabledAt,
            int failedAttempts
    ) {
        return AuthenticationAccount.reconstitute(
                42L,
                "password-hash",
                accessStatus,
                authenticationStatus,
                credentialStatus,
                null,
                CREATED_AT,
                accessDisabledAt,
                accessDisabledAt,
                null,
                failedAttempts
        );
    }
}
