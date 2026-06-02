package com.jeepclub.backend.billing.infra.persistence.mapper;

import com.jeepclub.backend.billing.core.domain.model.ChargeDefinition;
import com.jeepclub.backend.billing.infra.persistence.entity.ChargeDefinitionEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ChargeDefinitionMapper {

    public ChargeDefinition toDomain(ChargeDefinitionEntity entity) {
        Objects.requireNonNull(entity, "ChargeDefinitionEntity cannot be null");

        return ChargeDefinition.reconstitute(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getDefaultAmount(),
                entity.getRecurrenceType(),
                entity.getRequired(),
                entity.getPaymentAcceptancePolicy(),
                entity.getLatePaymentGraceDays(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getArchivedAt()
        );
    }

    public ChargeDefinitionEntity toEntity(ChargeDefinition chargeDefinition) {
        Objects.requireNonNull(chargeDefinition, "ChargeDefinition cannot be null");

        return new ChargeDefinitionEntity(
                chargeDefinition.getId(),
                chargeDefinition.getName(),
                chargeDefinition.getDescription(),
                chargeDefinition.getDefaultAmount(),
                chargeDefinition.getRecurrenceType(),
                chargeDefinition.getRequired(),
                chargeDefinition.getPaymentAcceptancePolicy(),
                chargeDefinition.getLatePaymentGraceDays(),
                chargeDefinition.getStatus(),
                chargeDefinition.getCreatedAt(),
                chargeDefinition.getUpdatedAt(),
                chargeDefinition.getArchivedAt()
        );
    }
}