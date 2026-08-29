package com.jeepclub.backend.authentication.core.domain.model;

import com.jeepclub.backend.authentication.core.domain.enums.AccountStatus;
import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationStatus;
import com.jeepclub.backend.authentication.core.domain.enums.CredentialStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class UserTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant NOW = Instant.parse("2026-01-02T00:00:00Z");

    @Test
    void disableAndEnablePreserveRequiredPasswordChange() {
        User user = user(CredentialStatus.CHANGE_REQUIRED);

        user.disable(NOW);
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.DISABLED);
        assertThat(user.getCredentialStatus()).isEqualTo(CredentialStatus.CHANGE_REQUIRED);

        user.enable(NOW.plusSeconds(1));
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(user.getCredentialStatus()).isEqualTo(CredentialStatus.CHANGE_REQUIRED);
        assertThat(user.isChangePasswordRequired()).isTrue();
    }

    @Test
    void unlockDoesNotEnableAdministrativelyDisabledAccount() {
        User user = user(
                AccountStatus.DISABLED,
                AuthenticationStatus.LOCKED,
                CredentialStatus.PERMANENT,
                NOW,
                5
        );

        user.unlock();

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.DISABLED);
        assertThat(user.getAuthenticationStatus()).isEqualTo(AuthenticationStatus.ENABLED);
    }

    @Test
    void permanentPasswordChangeOnlyChangesCredentialAndAuthenticationState() {
        User user = user(
                AccountStatus.DISABLED,
                AuthenticationStatus.LOCKED,
                CredentialStatus.CHANGE_REQUIRED,
                NOW,
                5
        );

        user.changePassword("new-hash", NOW.plusSeconds(1));

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.DISABLED);
        assertThat(user.getAuthenticationStatus()).isEqualTo(AuthenticationStatus.ENABLED);
        assertThat(user.getCredentialStatus()).isEqualTo(CredentialStatus.PERMANENT);
    }

    @Test
    void splitStatusesPreserveTheirIndependentMeaning() {
        User pendingFirstAccess = user(CredentialStatus.PENDING_FIRST_ACCESS);

        assertThat(pendingFirstAccess.getCredentialStatus())
                .isEqualTo(CredentialStatus.PENDING_FIRST_ACCESS);
        assertThat(pendingFirstAccess.isChangePasswordRequired()).isTrue();
        assertThat(user(CredentialStatus.CHANGE_REQUIRED).getCredentialStatus())
                .isEqualTo(CredentialStatus.CHANGE_REQUIRED);
        assertThat(user(
                AccountStatus.ACTIVE,
                AuthenticationStatus.LOCKED,
                CredentialStatus.CHANGE_REQUIRED,
                null,
                5
        ).getAuthenticationStatus()).isEqualTo(AuthenticationStatus.LOCKED);
    }

    @Test
    void reconstitutionRejectsImpossibleAccountTimestampCombination() {
        assertThatIllegalArgumentException().isThrownBy(() -> user(
                AccountStatus.DISABLED,
                AuthenticationStatus.ENABLED,
                CredentialStatus.PERMANENT,
                null,
                0
        ));
    }

    private User user(CredentialStatus credentialStatus) {
        return user(
                AccountStatus.ACTIVE,
                AuthenticationStatus.ENABLED,
                credentialStatus,
                null,
                0
        );
    }

    private User user(
            AccountStatus accountStatus,
            AuthenticationStatus authenticationStatus,
            CredentialStatus credentialStatus,
            Instant disabledAt,
            int failedAttempts
    ) {
        return User.reconstitute(
                1L,
                "Lucas Alves",
                null,
                "lucas@example.com",
                "52998224725",
                null,
                "hash",
                null,
                null,
                accountStatus,
                authenticationStatus,
                credentialStatus,
                null,
                CREATED_AT,
                disabledAt,
                null,
                null,
                failedAttempts
        );
    }
}
