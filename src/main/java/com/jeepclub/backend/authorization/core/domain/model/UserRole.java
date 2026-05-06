package com.jeepclub.backend.authorization.core.domain.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UserRole {

    private Long id;
    private Long userId;
    private Long roleId;
    private Instant createdAt;

    private UserRole(
            Long id,
            Long userId,
            Long roleId,
            Instant createdAt
    ) {
        this.id = id;
        this.userId = validateId(userId, "userId");
        this.roleId = validateId(roleId, "roleId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    public static UserRole create(
            Long userId,
            Long roleId,
            Instant now
    ) {
        Objects.requireNonNull(now, "now cannot be null");

        return new UserRole(
                null,
                userId,
                roleId,
                now
        );
    }

    public static UserRole reconstitute(
            Long id,
            Long userId,
            Long roleId,
            Instant createdAt
    ) {
        validateId(id, "id");

        return new UserRole(
                id,
                userId,
                roleId,
                createdAt
        );
    }

    private static Long validateId(
            Long id,
            String fieldName
    ) {
        if (id == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }

        if (id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }

        return id;
    }
}