package com.jeepclub.backend.authorization.core.domain.model;

import com.jeepclub.backend.authorization.core.domain.enums.ModuleCode;
import com.jeepclub.backend.authorization.core.domain.enums.PermissionCode;
import com.jeepclub.backend.authorization.core.domain.enums.PermissionDefinition;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Permission {

    private Long id;
    private PermissionCode code;
    private String description;
    private ModuleCode module;
    private Instant createdAt;
    private Instant updatedAt;

    private Permission(
            Long id,
            PermissionCode code,
            String description,
            ModuleCode module,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.code = Objects.requireNonNull(code, "Permission code cannot be null");
        this.description = Objects.requireNonNull(description, "Permission description cannot be null");
        this.module = Objects.requireNonNull(module, "Permission module cannot be null");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Permission fromDefinition(
            PermissionDefinition definition,
            Instant now
    ) {
        Objects.requireNonNull(definition, "Permission definition cannot be null");
        Objects.requireNonNull(now, "now cannot be null");

        return new Permission(
                null,
                definition.getCode(),
                definition.getDescription(),
                definition.getModule(),
                now,
                now
        );
    }

    public static Permission reconstitute(
            Long id,
            PermissionCode code,
            String description,
            ModuleCode module,
            Instant createdAt,
            Instant updatedAt
    ) {
        Objects.requireNonNull(id, "Permission id cannot be null when reconstituting");
        Objects.requireNonNull(createdAt, "Permission createdAt cannot be null when reconstituting");

        return new Permission(
                id,
                code,
                description,
                module,
                createdAt,
                updatedAt
        );
    }

    public boolean synchronizeWith(PermissionDefinition definition, Instant synchronizedAt) {
        Objects.requireNonNull(definition, "Permission definition cannot be null");
        Objects.requireNonNull(synchronizedAt, "Synchronized at cannot be null");

        if (this.code != definition.getCode()) {
            throw new IllegalArgumentException("Cannot synchronize permission with different code");
        }

        boolean changed = false;

        if (!Objects.equals(this.description, definition.getDescription())) {
            this.description = definition.getDescription();
            changed = true;
        }

        if (this.module != definition.getModule()) {
            this.module = definition.getModule();
            changed = true;
        }

        if (changed) {
            this.updatedAt = synchronizedAt;
        }

        return changed;
    }
}