package com.jeepclub.backend.billing.core.domain.model.assignment;

import com.jeepclub.backend.billing.core.domain.enums.assignment.ChargeAudienceType;

import java.time.Instant;

public final class AllMembersChargeAssignment extends ChargeAssignment {

    private AllMembersChargeAssignment(
            Long id,
            Long chargeDefinitionId,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        super(id, chargeDefinitionId, active, createdAt, updatedAt);
    }

    public static AllMembersChargeAssignment create(
            Long chargeDefinitionId,
            Instant now
    ) {
        return new AllMembersChargeAssignment(
                null,
                chargeDefinitionId,
                true,
                now,
                null
        );
    }

    public static AllMembersChargeAssignment reconstitute(
            Long id,
            Long chargeDefinitionId,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new AllMembersChargeAssignment(
                id,
                chargeDefinitionId,
                active,
                createdAt,
                updatedAt
        );
    }

    @Override
    public ChargeAudienceType audienceType() {
        return ChargeAudienceType.ALL_MEMBERS;
    }
}