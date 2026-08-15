package com.jeepclub.backend.authentication.core.application.query.user;

import com.jeepclub.backend.authentication.core.domain.enums.AccountStatus;
import com.jeepclub.backend.authentication.core.domain.enums.AuthenticationStatus;
import com.jeepclub.backend.authentication.core.domain.enums.CredentialStatus;
import java.time.Instant;

public record AdminUserFilter(
        Long id,
        String name,
        String cpf,
        String email,
        String phoneNumber,
        AccountStatus accountStatus,
        AuthenticationStatus authenticationStatus,
        CredentialStatus credentialStatus,
        Boolean passwordChangeRequired,
        Instant createdFrom,
        Instant createdTo,
        Instant updatedFrom,
        Instant updatedTo,
        String query
) {}