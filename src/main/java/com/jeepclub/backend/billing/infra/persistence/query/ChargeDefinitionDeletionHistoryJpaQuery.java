package com.jeepclub.backend.billing.infra.persistence.query;
import com.jeepclub.backend.billing.core.repository.ChargeDefinitionDeletionHistoryQueryRepository;
import com.jeepclub.backend.billing.infra.persistence.jpa.*;
@org.springframework.stereotype.Repository @lombok.RequiredArgsConstructor
public class ChargeDefinitionDeletionHistoryJpaQuery implements ChargeDefinitionDeletionHistoryQueryRepository {
    private final ChargeDefinitionDeletionHistoryJpaRepository history;
    public java.util.Optional<ChargeDefinitionDeletionHistorySnapshot> findByOriginalId(Long id) {
        return history.findById(id).map(e -> new ChargeDefinitionDeletionHistorySnapshot(e.getOriginalId(), e.getName(), e.getDescription(), e.getDefaultAmount(), e.getRecurrenceType(), e.getRequired(), e.getPaymentAcceptancePolicy(), e.getLatePaymentGraceDays(), e.getPreviousStatus(), e.getCreatedAt(), e.getUpdatedAt(), e.getArchivedAt(), e.getDeletedByUserId(), e.getDeletedAt(), e.getReason(), e.getSchemaVersion()));
    }
}
