package com.jeepclub.backend.tools.core.domain.model;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import com.jeepclub.backend.tools.core.domain.exception.ToolAccessDeniedException;
import com.jeepclub.backend.tools.core.domain.exception.ToolLifecycleConflictException;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
public class Tool {

    private Long id;
    private String name;
    private String description;
    private ToolStatus status;
    private Long userId;
    private String photoStorageKey;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime archivedAt;

    private Tool(
            Long id,
            String name,
            String description,
            ToolStatus status,
            Long userId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.status = status;
        this.userId = userId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Tool create(
            String name,
            String description,
            Long userId,
            LocalDateTime createdAt
    ) {
        Objects.requireNonNull(createdAt, "createdAt cannot be null");
        return new Tool(null, name, description, ToolStatus.ACTIVE, userId, createdAt, createdAt);
    }

    public static Tool reconstitute(
            Long id,
            String name,
            String description,
            ToolStatus status,
            Long userId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return reconstitute(id, name, description, status, userId, createdAt, updatedAt, null);
    }

    public static Tool reconstitute(
            Long id,
            String name,
            String description,
            ToolStatus status,
            Long userId,
            LocalDateTime createdAt,
            LocalDateTime updatedAt,
            LocalDateTime archivedAt
    ) {
        if ((status == ToolStatus.ARCHIVED) != (archivedAt != null)) {
            throw new IllegalArgumentException("Archived state requires its archival timestamp only.");
        }
        if (archivedAt != null && (createdAt == null || archivedAt.isBefore(createdAt))) {
            throw new IllegalArgumentException("archivedAt cannot be before createdAt.");
        }

        Tool tool = new Tool(id, name, description, status, userId, createdAt, updatedAt);
        tool.archivedAt = archivedAt;
        return tool;
    }

    public void updateDetails(String name, String description, LocalDateTime updatedAt) {
        assertNotArchived();
        Objects.requireNonNull(updatedAt, "updatedAt cannot be null");

        if (name != null && !name.isBlank()) {
            this.name = name.trim();
        }
        if (description != null) {
            this.description = description.trim();
        }
        this.updatedAt = updatedAt;
    }

    public void updatePhoto(String storageKey, LocalDateTime updatedAt) {
        assertNotArchived();
        Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
        this.photoStorageKey = storageKey == null ? null
                : com.jeepclub.backend.shared.storage.ImageReference.require(storageKey);
        this.updatedAt = updatedAt;
    }

    public void restorePhoto(String storageKey) {
        this.photoStorageKey = storageKey;
    }

    public boolean activate(LocalDateTime updatedAt) {
        assertNotArchived();
        Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
        if (this.status == ToolStatus.ACTIVE) {
            return false;
        }

        this.status = ToolStatus.ACTIVE;
        this.updatedAt = updatedAt;
        return true;
    }

    public boolean deactivate(LocalDateTime updatedAt) {
        assertNotArchived();
        Objects.requireNonNull(updatedAt, "updatedAt cannot be null");
        if (this.status == ToolStatus.INACTIVE) {
            return false;
        }

        this.status = ToolStatus.INACTIVE;
        this.updatedAt = updatedAt;
        return true;
    }

    public void assertBelongsTo(Long currentUserId) {
        if (!this.userId.equals(currentUserId)) {
            throw new ToolAccessDeniedException("Acesso negado: Esta ferramenta não pertence a você.");
        }
    }

    public boolean archive(LocalDateTime now) {
        Objects.requireNonNull(now, "now cannot be null");
        if (now.isBefore(createdAt)) throw new IllegalArgumentException("now cannot be before createdAt.");
        if (status == ToolStatus.ARCHIVED) return false;
        status = ToolStatus.ARCHIVED;
        archivedAt = now;
        updatedAt = now;
        return true;
    }

    public boolean unarchive(LocalDateTime now) {
        Objects.requireNonNull(now, "now cannot be null");
        if (now.isBefore(createdAt)) throw new IllegalArgumentException("now cannot be before createdAt.");
        if (status == ToolStatus.INACTIVE) return false;
        if (status != ToolStatus.ARCHIVED) {
            throw new ToolLifecycleConflictException("Only an archived record can be unarchived.");
        }
        status = ToolStatus.INACTIVE;
        archivedAt = null;
        updatedAt = now;
        return true;
    }

    private void assertNotArchived() {
        if (status == ToolStatus.ARCHIVED) {
            throw new ToolLifecycleConflictException("Archived record requires explicit unarchive before this operation.");
        }
    }
}
