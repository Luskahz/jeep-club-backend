package com.jeepclub.backend.authorization.core.domain.model;

import com.jeepclub.backend.authorization.core.domain.enums.RoleStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Role {

    private Long id;
    private String name;
    private String description;
    private RoleStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    private Role(
            Long id,
            String name,
            String description,
            RoleStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        this.id = id;
        this.name = validateName(name);
        this.description = normalizeDescription(description);
        this.status = Objects.requireNonNull(status, "Role status cannot be null");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static Role create(
            String name,
            String description
    ) {
        return new Role(
                null,
                name,
                description,
                RoleStatus.ACTIVE,
                null,
                null,
                null
        );
    }

    public static Role reconstitute(
            Long id,
            String name,
            String description,
            RoleStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        Objects.requireNonNull(id, "Role id cannot be null when reconstituting");
        Objects.requireNonNull(createdAt, "Role createdAt cannot be null when reconstituting");

        return new Role(
                id,
                name,
                description,
                status,
                createdAt,
                updatedAt,
                deletedAt
        );
    }

    public void update(
            String name,
            String description,
            Instant updatedAt
    ) {
        ensureNotDeleted();
        Objects.requireNonNull(updatedAt, "updatedAt cannot be null");

        this.name = validateName(name);
        this.description = normalizeDescription(description);
        this.updatedAt = updatedAt;
    }

    public void activate(Instant updatedAt) {
        ensureNotDeleted();
        Objects.requireNonNull(updatedAt, "updatedAt cannot be null");

        this.status = RoleStatus.ACTIVE;
        this.updatedAt = updatedAt;
    }

    public void deactivate(Instant updatedAt) {
        ensureNotDeleted();
        Objects.requireNonNull(updatedAt, "updatedAt cannot be null");

        this.status = RoleStatus.INACTIVE;
        this.updatedAt = updatedAt;
    }

    public void delete(Instant deletedAt) {
        ensureNotDeleted();
        Objects.requireNonNull(deletedAt, "deletedAt cannot be null");

        this.status = RoleStatus.DELETED;
        this.deletedAt = deletedAt;
        this.updatedAt = deletedAt;
    }

    public boolean isActive() {
        return this.status == RoleStatus.ACTIVE && this.deletedAt == null;
    }

    public boolean isDeleted() {
        return this.status == RoleStatus.DELETED || this.deletedAt != null;
    }

    private void ensureNotDeleted() {
        if (isDeleted()) {
            throw new IllegalStateException("Cannot change a deleted role");
        }
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Role name cannot be blank");
        }

        String normalizedName = name.trim();

        if (normalizedName.length() > 100) {
            throw new IllegalArgumentException("Role name cannot exceed 100 characters");
        }

        return normalizedName;
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        String normalizedDescription = description.trim();

        if (normalizedDescription.length() > 255) {
            throw new IllegalArgumentException("Role description cannot exceed 255 characters");
        }

        return normalizedDescription;
    }
}