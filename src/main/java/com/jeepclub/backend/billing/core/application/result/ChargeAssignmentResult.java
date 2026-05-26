package com.jeepclub.backend.billing.core.application.result;

import com.jeepclub.backend.billing.core.domain.enums.ChargeAssignmentType;
import com.jeepclub.backend.billing.core.domain.model.ChargeAssignment;

import java.time.Instant;
import java.util.Objects;

public record ChargeAssignmentResult(
        Long id,
        Long chargeDefinitionId,
        ChargeAssignmentType assignmentType,
        Long targetId,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public static ChargeAssignmentResult from(ChargeAssignment chargeAssignment) {
        Objects.requireNonNull(chargeAssignment, "chargeAssignment cannot be null");

        return new ChargeAssignmentResult(
                chargeAssignment.getId(),
                chargeAssignment.getChargeDefinitionId(),
                chargeAssignment.getAssignmentType(),
                chargeAssignment.getTargetId(),
                chargeAssignment.isActive(),
                chargeAssignment.getCreatedAt(),
                chargeAssignment.getUpdatedAt()
        );
    }
}