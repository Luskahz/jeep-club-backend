package com.jeepclub.backend.billing.core.application.result;

import com.jeepclub.backend.billing.core.domain.enums.assignment.ChargeAudienceType;
import com.jeepclub.backend.billing.core.domain.model.assignment.AllMembersChargeAssignment;
import com.jeepclub.backend.billing.core.domain.model.assignment.ChargeAssignment;
import com.jeepclub.backend.billing.core.domain.model.assignment.EventParticipantsChargeAssignment;
import com.jeepclub.backend.billing.core.domain.model.assignment.RoleChargeAssignment;
import com.jeepclub.backend.billing.core.domain.model.assignment.UserChargeAssignment;

import java.time.Instant;
import java.util.Objects;

public record ChargeAssignmentResult(
        Long id,
        Long chargeDefinitionId,
        ChargeAudienceType audienceType,
        Long userId,
        Long roleId,
        Long eventId,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public static ChargeAssignmentResult from(ChargeAssignment chargeAssignment) {
        Objects.requireNonNull(chargeAssignment, "chargeAssignment cannot be null");

        if (chargeAssignment instanceof AllMembersChargeAssignment assignment) {
            return new ChargeAssignmentResult(
                    assignment.getId(),
                    assignment.getChargeDefinitionId(),
                    assignment.audienceType(),
                    null,
                    null,
                    null,
                    assignment.isActive(),
                    assignment.getCreatedAt(),
                    assignment.getUpdatedAt()
            );
        }

        if (chargeAssignment instanceof UserChargeAssignment assignment) {
            return new ChargeAssignmentResult(
                    assignment.getId(),
                    assignment.getChargeDefinitionId(),
                    assignment.audienceType(),
                    assignment.getUserId(),
                    null,
                    null,
                    assignment.isActive(),
                    assignment.getCreatedAt(),
                    assignment.getUpdatedAt()
            );
        }

        if (chargeAssignment instanceof RoleChargeAssignment assignment) {
            return new ChargeAssignmentResult(
                    assignment.getId(),
                    assignment.getChargeDefinitionId(),
                    assignment.audienceType(),
                    null,
                    assignment.getRoleId(),
                    null,
                    assignment.isActive(),
                    assignment.getCreatedAt(),
                    assignment.getUpdatedAt()
            );
        }

        if (chargeAssignment instanceof EventParticipantsChargeAssignment assignment) {
            return new ChargeAssignmentResult(
                    assignment.getId(),
                    assignment.getChargeDefinitionId(),
                    assignment.audienceType(),
                    null,
                    null,
                    assignment.getEventId(),
                    assignment.isActive(),
                    assignment.getCreatedAt(),
                    assignment.getUpdatedAt()
            );
        }

        throw new IllegalArgumentException(
                "Unsupported charge assignment type: " + chargeAssignment.getClass().getName()
        );
    }
}