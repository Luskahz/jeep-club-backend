package com.jeepclub.backend.authentication.core.application.query.user;

import com.jeepclub.backend.authentication.core.domain.enums.UserStatus;

import java.time.Instant;

public record AdminUserFilter(
        Long id,
        String name,
        String cpf,
        String email,
        String phone,
        UserStatus status,
        Boolean passwordChangeRequired,
        Instant createdFrom,
        Instant createdTo,
        Instant updatedFrom,
        Instant updatedTo,
        String query
) {
}