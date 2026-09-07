package com.jeepclub.backend.health.core.application.audit;

import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;

import java.time.Instant;

/**
 * Audit metadata only. Clinical fields must never be added to this contract.
 */
public record MedicalProfileAuditEvent(
        Long actorUserId,
        MedicalProfileOwnerType ownerType,
        Long ownerId,
        MedicalProfileAuditOperation operation,
        MedicalProfileAuditOutcome outcome,
        Instant occurredAt
) {
}
