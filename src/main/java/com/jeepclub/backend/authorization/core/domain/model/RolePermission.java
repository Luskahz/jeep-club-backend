package com.jeepclub.backend.authorization.core.domain.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class RolePermission {

    private Long id;
    private Long roleId;
    private Long permissionId;
    private Instant createdAt;

    private RolePermission(
            Long id,
            Long roleId,
            Long permissionId,
            Instant createdAt
    ) {
        this.id = id;
        this.roleId = validateId(roleId, "roleId");
        this.permissionId = validateId(permissionId, "permissionId");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
    }

    public static RolePermission create(
            Long roleId,
            Long permissionId,
            Instant now
    ) {
        Objects.requireNonNull(now, "now cannot be null");

        return new RolePermission(
                null,
                roleId,
                permissionId,
                now
        );
    }

    public static RolePermission reconstitute(
            Long id,
            Long roleId,
            Long permissionId,
            Instant createdAt
    ) {
        validateId(id, "id");

        return new RolePermission(
                id,
                roleId,
                permissionId,
                createdAt
        );
    }

    public boolean belongsToRole(Long roleId) {
        return Objects.equals(this.roleId, roleId);
    }

    public boolean referencesPermission(Long permissionId) {
        return Objects.equals(this.permissionId, permissionId);
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