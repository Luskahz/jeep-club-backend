package com.jeepclub.backend.authentication.core.application.result.admin.user;

import com.jeepclub.backend.authentication.core.domain.enums.UserStatus;
import com.jeepclub.backend.authentication.core.domain.model.User;

import java.time.Instant;
import java.util.Objects;

public record AdminUserResult(
        Long id,
        String name,
        String cpf,
        String email,
        String phone,
        UserStatus status,
        Boolean passwordChangeRequired,
        Instant createdAt,
        Instant updatedAt
) {

    public static AdminUserResult from(User user) {
        Objects.requireNonNull(user, "user cannot be null");

        return new AdminUserResult(
                user.getId(),
                user.getName(),
                user.getCpf(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getStatus(),
                user.isChangePasswordRequired(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}