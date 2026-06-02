package com.jeepclub.backend.billing.core.application.result;

import com.jeepclub.backend.billing.core.domain.enums.definition.ChargeDefinitionStatus;
import com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType;
import com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy;
import com.jeepclub.backend.billing.core.domain.model.ChargeDefinition;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;


public record ChargeDefinitionResult(
        Long id,
        String name,
        String description,
        BigDecimal defaultAmount,
        ChargeRecurrenceType recurrenceType,
        Boolean required,
        PaymentAcceptancePolicy paymentAcceptancePolicy,
        Integer latePaymentGraceDays,
        ChargeDefinitionStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant archivedAt
) {

    public static ChargeDefinitionResult from(ChargeDefinition chargeDefinition) {
        Objects.requireNonNull(chargeDefinition, "chargeDefinition cannot be null");

        return new ChargeDefinitionResult(
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