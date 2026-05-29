package com.jeepclub.backend.billing.core.application.result.cycle;

import com.jeepclub.backend.billing.core.domain.enums.ChargeCycleStatus;
import com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType;
import com.jeepclub.backend.billing.core.domain.model.ChargeCycle;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record ChargeCycleResult(
        Long id,
        Long chargeDefinitionId,
        String chargeDefinitionNameSnapshot,
        String chargeDefinitionDescriptionSnapshot,
        BigDecimal chargeDefinitionDefaultAmountSnapshot,
        ChargeRecurrenceType chargeDefinitionRecurrenceTypeSnapshot,
        Boolean chargeDefinitionRequiredSnapshot,
        String code,
        LocalDate dueDate,
        ChargeCycleStatus status,
        Long generatedByUserId,
        Instant generatedAt,
        Instant canceledAt,
        Long canceledByUserId,
        Instant finishedAt,
        Long finishedByUserId,
        Instant archivedAt,
        Long archivedByUserId,
        Instant createdAt,
        Instant updatedAt
) {

    public static ChargeCycleResult from(ChargeCycle chargeCycle) {
        Objects.requireNonNull(chargeCycle, "chargeCycle cannot be null");

        return new ChargeCycleResult(
                chargeCycle.getId(),
                chargeCycle.getChargeDefinitionId(),
                chargeCycle.getChargeDefinitionNameSnapshot(),
                chargeCycle.getChargeDefinitionDescriptionSnapshot(),
                chargeCycle.getChargeDefinitionDefaultAmountSnapshot(),
                chargeCycle.getChargeDefinitionRecurrenceTypeSnapshot(),
                chargeCycle.getChargeDefinitionRequiredSnapshot(),
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