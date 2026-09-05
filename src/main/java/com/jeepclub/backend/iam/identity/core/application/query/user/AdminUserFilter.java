package com.jeepclub.backend.iam.identity.core.application.query.user;

import com.jeepclub.backend.iam.identity.api.module.UserStatus;

import java.time.Instant;
import java.time.LocalDate;

public record AdminUserFilter(
        Long id,
        String name,
        LocalDate birthDate,
        String email,
        String cpf,
        String rg,
        String phoneNumber,
        UserStatus status,
        Instant createdFrom,
        Instant createdTo,
        Instant updatedFrom,
        Instant updatedTo,
        String query
) {
}
