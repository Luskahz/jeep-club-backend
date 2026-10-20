package com.jeepclub.backend.iam.authorization.core.domain.model;

import com.jeepclub.backend.iam.authorization.core.domain.enums.RoleKind;
import com.jeepclub.backend.iam.authorization.core.domain.enums.RoleStatus;
import com.jeepclub.backend.iam.authorization.core.domain.exception.role.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Role {

    private static final int MAX_NAME_LENGTH = 100;
    private static final int MAX_DESCRIPTION_LENGTH = 255;

    private Long id;
    private String name;
    private String description;
    private RoleKind kind;
    private RoleStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;
    private Instant archivedAt;

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
        this(id, name, description, kind, status, createdAt, updatedAt, deletedAt, null);
    }

    private Role(
            Long id,
            String name,
            String description,
            RoleKind kind,
            RoleStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            Instant archivedAt
    ) {
        this.id = id;
        this.name = validateName(name);
        this.description = normalizeDescription(description);
        this.kind = Objects.requireNonNull(kind, "Role kind cannot be null");
        this.status = Objects.requireNonNull(status, "Role status cannot be null");
        this.createdAt = Objects.requireNonNull(
                createdAt,
                "Role createdAt cannot be null"
        );
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.archivedAt = archivedAt;

        validateDeletionConsistency();
        if ((status == RoleStatus.ARCHIVED) != (archivedAt != null)) {
            throw new IllegalArgumentException("Archived role requires its archival timestamp only.");
        }
        if (archivedAt != null && archivedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("archivedAt cannot be before createdAt.");
        }
        validateRootConsistency();
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
        return reconstitute(id, name, description, kind, status, createdAt, updatedAt, deletedAt, null);
    }

    public static Role reconstitute(
            Long id,
            String name,
            String description,
            RoleKind kind,
            RoleStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt,
            Instant archivedAt
    ) {
        Objects.requireNonNull(
                id,
                "Role id cannot be null when reconstituting"
        );

        return new Role(
                id,
                name,
                description,
                kind,
                status,
                createdAt,
                updatedAt,
                deletedAt,
                archivedAt
        );
    }

    public boolean update(
            String name,
            String description,
            Instant updatedAt
    ) {
        ensureCanBeChanged();
        Objects.requireNonNull(updatedAt, "updatedAt cannot be null");

        String normalizedName = validateName(name);
        String normalizedDescription = normalizeDescription(description);

        boolean unchanged =
                Objects.equals(this.name, normalizedName)
                        && Objects.equals(
                        this.description,
                        normalizedDescription
                );

        if (unchanged) {
            return false;
        }

        this.name = normalizedName;
        this.description = normalizedDescription;
        this.updatedAt = updatedAt;

        return true;
    }

    public boolean activate(Instant updatedAt) {
        ensureCanBeChanged();
        Objects.requireNonNull(updatedAt, "updatedAt cannot be null");

        if (this.status == RoleStatus.ACTIVE) {
            return false;
        }

        this.status = RoleStatus.ACTIVE;
        this.updatedAt = updatedAt;

        return true;
    }

    public boolean deactivate(Instant updatedAt) {
        ensureCanBeChanged();
        Objects.requireNonNull(updatedAt, "updatedAt cannot be null");

        if (this.status == RoleStatus.INACTIVE) {
            return false;
        }

        this.status = RoleStatus.INACTIVE;
        this.updatedAt = updatedAt;

        return true;
    }

    public void ensureCanBeChanged() {
        ensureCustom();
        if (status == RoleStatus.ARCHIVED) {
            throw new RoleLifecycleConflictException("Archived role requires explicit unarchive.");
        }
    }

    public void ensureCanBeDeleted() {
        ensureCustom();
    }

    private void ensureCustom() {
        ensureNotDeleted();
        if (isRoot()) {
            throw new RootRoleCannotBeChangedException(this.id);
        }
    }

    public void ensureActive() {
        ensureNotDeleted();

        if (this.status != RoleStatus.ACTIVE) {
            throw new InactiveRoleCannotBeUsedException(this.id);
        }
    }

    public boolean isRoot() {
        return this.kind == RoleKind.ROOT;
    }

    public boolean isCustom() {
        return this.kind == RoleKind.CUSTOM;
    }

    public boolean isActive() {
        return this.status == RoleStatus.ACTIVE
                && this.deletedAt == null;
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    public static String normalizeName(String name) {
        return validateName(name);
    }

    private void ensureNotDeleted() {
        if (isDeleted()) {
            throw new DeletedRoleCannotBeChangedException(this.id);
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

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }

        String normalizedDescription = description.trim();

        if (normalizedDescription.length() > MAX_DESCRIPTION_LENGTH) {
            throw new RoleDescriptionTooLongException(
                    MAX_DESCRIPTION_LENGTH
            );
        }

        return normalizedDescription;
    }

    private void validateDeletionConsistency() {
        if (deletedAt != null) {
            throw new IllegalStateException("Deleted roles belong to segregated deletion history.");
        }
    }

    private void validateRootConsistency() {
        if (!isRoot()) {
            return;
        }

        if (this.status != RoleStatus.ACTIVE) {
            throw new IllegalStateException(
                    "ROOT role must always be ACTIVE."
            );
        }

        if (this.deletedAt != null) {
            throw new IllegalStateException(
                    "ROOT role cannot be deleted."
            );
        }
    }

    public boolean archive(Instant now) {
        ensureCustom();
        validateLifecycleInstant(now);
        if (status == RoleStatus.ARCHIVED) return false;
        status = RoleStatus.ARCHIVED;
        archivedAt = now;
        updatedAt = now;
        return true;
    }

    public boolean unarchive(Instant now) {
        ensureCustom();
        validateLifecycleInstant(now);
        if (status == RoleStatus.INACTIVE) return false;
        if (status != RoleStatus.ARCHIVED) {
            throw new RoleLifecycleConflictException("Only an archived role can be unarchived.");
        }
        status = RoleStatus.INACTIVE;
        archivedAt = null;
        updatedAt = now;
        return true;
    }

    private void validateLifecycleInstant(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");
        if (now.isBefore(createdAt)) throw new IllegalArgumentException("now cannot be before createdAt.");
    }
}
