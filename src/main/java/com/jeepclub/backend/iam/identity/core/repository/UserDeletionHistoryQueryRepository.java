package com.jeepclub.backend.iam.identity.core.repository;
import java.time.Instant;
import java.math.BigDecimal;
public interface UserDeletionHistoryQueryRepository {

    record UserDeletionHistorySnapshot(Long originalId,
            String previousStatus,
            Instant createdAt,
            Instant disabledAt,
            Instant archivedAt,
            Instant updatedAt,
            Long deletedByUserId,
            Instant deletedAt,
            String reason,
            int schemaVersion) {}
    java.util.Optional<UserDeletionHistorySnapshot> findByOriginalId(Long id);
}
