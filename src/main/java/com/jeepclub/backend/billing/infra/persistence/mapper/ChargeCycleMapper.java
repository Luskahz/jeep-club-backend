package com.jeepclub.backend.billing.infra.persistence.mapper;

import com.jeepclub.backend.billing.core.domain.model.ChargeCycle;
import com.jeepclub.backend.billing.infra.persistence.entity.ChargeCycleEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ChargeCycleMapper {

    public ChargeCycle toDomain(ChargeCycleEntity entity) {
        Objects.requireNonNull(entity, "ChargeCycleEntity cannot be null");

        return ChargeCycle.reconstitute(
                entity.getId(),
                entity.getChargeDefinitionId(),
                entity.getChargeDefinitionNameSnapshot(),
                entity.getChargeDefinitionDescriptionSnapshot(),
                entity.getChargeDefinitionDefaultAmountSnapshot(),
                entity.getChargeDefinitionRecurrenceTypeSnapshot(),
                entity.getChargeDefinitionRequiredSnapshot(),
                entity.getChargeDefinitionPaymentAcceptancePolicySnapshot(),
                entity.getChargeDefinitionLatePaymentGraceDaysSnapshot(),
                entity.getCode(),
                entity.getDueDate(),
                entity.getStatus(),
                entity.getGeneratedByUserId(),
                entity.getGeneratedAt(),
                entity.getCanceledAt(),
                entity.getCanceledByUserId(),
                entity.getFinishedAt(),
                entity.getFinishedByUserId(),
                entity.getArchivedAt(),
                entity.getArchivedByUserId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ChargeCycleEntity toEntity(ChargeCycle chargeCycle) {
        Objects.requireNonNull(chargeCycle, "ChargeCycle cannot be null");

        return new ChargeCycleEntity(
                chargeCycle.getId(),
                chargeCycle.getChargeDefinitionId(),
                chargeCycle.getChargeDefinitionNameSnapshot(),
                chargeCycle.getChargeDefinitionDescriptionSnapshot(),
                chargeCycle.getChargeDefinitionDefaultAmountSnapshot(),
                chargeCycle.getChargeDefinitionRecurrenceTypeSnapshot(),
                chargeCycle.getChargeDefinitionRequiredSnapshot(),
                chargeCycle.getChargeDefinitionPaymentAcceptancePolicySnapshot(),
                chargeCycle.getChargeDefinitionLatePaymentGraceDaysSnapshot(),
                chargeCycle.getCode(),
                chargeCycle.getDueDate(),
                chargeCycle.getStatus(),
                chargeCycle.getGeneratedByUserId(),
                chargeCycle.getGeneratedAt(),
                chargeCycle.getCanceledAt(),
                chargeCycle.getCanceledByUserId(),
                chargeCycle.getFinishedAt(),
                chargeCycle.getFinishedByUserId(),
                chargeCycle.getArchivedAt(),
                chargeCycle.getArchivedByUserId(),
                chargeCycle.getCreatedAt(),
                chargeCycle.getUpdatedAt()
        );
    }
}