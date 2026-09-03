package com.jeepclub.backend.authentication.core.application.result.admin.user;

import com.jeepclub.backend.authentication.core.domain.enums.AccountStatus;
import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationStatus;
import com.jeepclub.backend.authentication.core.domain.enums.CredentialStatus;
import com.jeepclub.backend.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.identity.api.module.IdentityDetails;

import java.time.Instant;
import java.util.Objects;

public record AdminUserResult(
        Long id,
        String name,
        String cpf,
        String email,
        String phone,
        AccountStatus accountStatus,
        AuthenticationStatus authenticationStatus,
        CredentialStatus credentialStatus,
        Boolean passwordChangeRequired,
        Instant createdAt,
        Instant updatedAt
) {

    public static AdminUserResult from(
            IdentityDetails identity,
            AuthenticationAccount account
    ) {
        Objects.requireNonNull(identity, "identity cannot be null");
        Objects.requireNonNull(account, "account cannot be null");

        Instant updatedAt = latest(identity.updatedAt(), account.getUpdatedAt());

        return new AdminUserResult(
                identity.id(), identity.name(), identity.cpf(), identity.email(),
                identity.phoneNumber(), identity.administrativelyActive()
                        ? AccountStatus.ACTIVE : AccountStatus.DISABLED,
                account.getAuthenticationStatus(), account.getCredentialStatus(),
                account.isChangePasswordRequired(), identity.createdAt(), updatedAt
        );
    }

    private static Instant latest(Instant first, Instant second) {
        if (first == null) return second;
        if (second == null) return first;
        return first.isAfter(second) ? first : second;
    }
}
