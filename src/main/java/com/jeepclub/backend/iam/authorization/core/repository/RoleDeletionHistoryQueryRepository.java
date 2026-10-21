package com.jeepclub.backend.iam.authorization.core.repository;
import java.time.Instant;
import java.math.BigDecimal;
public interface RoleDeletionHistoryQueryRepository {

    record RoleDeletionHistorySnapshot(Long originalId,
            String name,
            String description,
            String kind,
            String previousStatus,
            Instant createdAt,
            Instant updatedAt,
            Instant archivedAt,
            Long deletedByUserId,
            Instant deletedAt,
            String reason,
            boolean inferredTime,
            int schemaVersion, java.util.List<RoleDeletionHistoryPermission> permissions, java.util.List<RoleDeletionHistoryBinding> bindings) {}

    record RoleDeletionHistoryPermission(Long originalId, Long permissionId, String permissionCode, Instant createdAt) {}

    record RoleDeletionHistoryBinding(Long originalId, Long userId, Instant createdAt) {}
    java.util.Optional<RoleDeletionHistorySnapshot> findByOriginalId(Long id);
}
