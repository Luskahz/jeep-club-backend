package com.jeepclub.backend.authorization.core.domain.model;

import com.jeepclub.backend.authorization.core.domain.enums.RoleKind;
import com.jeepclub.backend.authorization.core.domain.enums.RoleStatus;
import com.jeepclub.backend.authorization.core.domain.exception.role.*;
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
    private RoleKind kind;
    private RoleStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 255;

    private Role(
            Long id,
            String name,
            String description,
            RoleKind kind,
            RoleStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        this.id = id;
        this.name = validateName(name);
        this.description = normalizeDescription(description);
        this.kind = Objects.requireNonNull(kind, "Role kind cannot be null");
        this.status = Objects.requireNonNull(status, "Role status cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "Role createdAt cannot be null");
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;

        validateDeletionConsistency();
    }

    public static Role create(
            String name,
            String description,
            Instant now
    ) {
        Objects.requireNonNull(now, "now cannot be null");

        return new Role(
                null,
                name,
                description,
                RoleKind.CUSTOM,
                RoleStatus.ACTIVE,
                now,
                now,
                null
        );
    }

    public static Role createRoot(
            String name,
            String description,
            Instant now
    ) {
        Objects.requireNonNull(now, "now cannot be null");

        return new Role(
                null,
                name,
                description,
                RoleKind.ROOT,
                RoleStatus.ACTIVE,
                now,
                now,
                null
        );
    }

    public static Role reconstitute(
            Long id,
            String name,
            String description,
            RoleKind kind,
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
                kind,
                status,
                createdAt,
                updatedAt,
                deletedAt
        );
    }
    public boolean update(
            String name,
            String description,
            Instant updatedAt
    ) {
        ensureNotDeleted();
        Objects.requireNonNull(updatedAt, "updatedAt cannot be null");

        String normalizedName = validateName(name);
        String normalizedDescription = normalizeDescription(description);

        boolean unchanged =
                Objects.equals(this.name, normalizedName)
                        && Objects.equals(this.description, normalizedDescription);

        if (unchanged) {
            return false;
        }

        this.name = normalizedName;
        this.description = normalizedDescription;
        this.updatedAt = updatedAt;

        return true;
    }

    public boolean activate(Instant updatedAt) {
        ensureNotDeleted();
        Objects.requireNonNull(updatedAt, "updatedAt cannot be null");

        if (this.status == RoleStatus.ACTIVE) {
            return false;
        }

        this.status = RoleStatus.ACTIVE;
        this.updatedAt = updatedAt;
        return true;
    }

    public boolean deactivate(Instant updatedAt) {
        ensureNotDeleted();
        Objects.requireNonNull(updatedAt, "updatedAt cannot be null");

        if (this.status == RoleStatus.INACTIVE) {
            return false;
        }

        this.status = RoleStatus.INACTIVE;
        this.updatedAt = updatedAt;

        return true;
    }

    public void delete(Instant deletedAt) {
        ensureNotDeleted();
        Objects.requireNonNull(deletedAt, "deletedAt cannot be null");

        this.status = RoleStatus.DELETED;
        this.deletedAt = deletedAt;
        this.updatedAt = deletedAt;
    }

    public boolean isRoot() {
        return this.kind == RoleKind.ROOT;
    }

    public boolean isCustom() {
        return this.kind == RoleKind.CUSTOM;
    }

    public boolean isActive() {
        return this.status == RoleStatus.ACTIVE && this.deletedAt == null;
    }

    public boolean isDeleted() {
        return this.status == RoleStatus.DELETED || this.deletedAt != null;
    }

    private void ensureNotDeleted() {
        if (isDeleted()) {
            throw new DeletedRoleCannotBeChangedException(this.id);
        }
    }
    public void ensureActive() {
        ensureNotDeleted();

        if (this.status != RoleStatus.ACTIVE) {
            throw new InactiveRoleCannotBeUsedException(this.id);
        }
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new RoleNameCannotBeBlankException();
        }

        String normalizedName = name.trim();

        if (normalizedName.length() > MAX_NAME_LENGTH) {
            throw new RoleNameTooLongException(MAX_NAME_LENGTH);
        }

        return normalizedName;
    }

    public void ensureCanBeChanged() {
        ensureNotDeleted();
    }

    public static String normalizeName(String name) {
        return validateName(name);
    }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        String normalizedDescription = description.trim();

        if (normalizedDescription.length() > MAX_DESCRIPTION_LENGTH) {
            throw new RoleDescriptionTooLongException(MAX_DESCRIPTION_LENGTH);
        }

        return normalizedDescription;
    }

    private void validateDeletionConsistency() {
        if (this.status == RoleStatus.DELETED && this.deletedAt == null) {
            throw new IllegalStateException("Deleted role must have deletedAt.");
        }

        if (this.deletedAt != null && this.status != RoleStatus.DELETED) {
            throw new IllegalStateException("Role with deletedAt must have DELETED status.");
        }
    }
}