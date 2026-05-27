package com.jeepclub.backend.billing.core.application.result.cycle;

import com.jeepclub.backend.billing.core.domain.enums.ChargeCycleStatus;
import com.jeepclub.backend.billing.core.domain.model.ChargeCycle;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record ChargeCycleResult(
        Long id,
        Long chargeDefinitionId,
        String code,
        LocalDate dueDate,
        ChargeCycleStatus status,
        Long generatedByUserId,
        Instant generatedAt,
        Instant canceledAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static ChargeCycleResult from(ChargeCycle chargeCycle) {
        Objects.requireNonNull(chargeCycle, "chargeCycle cannot be null");

        return new ChargeCycleResult(
                chargeCycle.getId(),
                chargeCycle.getChargeDefinitionId(),
                chargeCycle.getCode(),
                chargeCycle.getDueDate(),
                chargeCycle.getStatus(),
                chargeCycle.getGeneratedByUserId(),
                chargeCycle.getGeneratedAt(),
                chargeCycle.getCanceledAt(),
                chargeCycle.getCreatedAt(),
                chargeCycle.getUpdatedAt()
        );
    }
}