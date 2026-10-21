package com.jeepclub.backend.billing.core.repository;
import java.time.Instant;
import java.math.BigDecimal;
public interface ChargeDefinitionDeletionHistoryQueryRepository {

    record ChargeDefinitionDeletionHistorySnapshot(Long originalId,
            String name,
            String description,
            BigDecimal defaultAmount,
            String recurrenceType,
            Boolean required,
            String paymentAcceptancePolicy,
            Integer latePaymentGraceDays,
            String previousStatus,
            Instant createdAt,
            Instant updatedAt,
            Instant archivedAt,
            Long deletedByUserId,
            Instant deletedAt,
            String reason,
            int schemaVersion) {}
    java.util.Optional<ChargeDefinitionDeletionHistorySnapshot> findByOriginalId(Long id);
}
