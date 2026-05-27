package com.jeepclub.backend.billing.core.domain.model.chargeAssignment;

import com.jeepclub.backend.billing.core.domain.enums.ChargeAudienceType;
import lombok.Getter;

import java.time.Instant;

@Getter
public final class RoleChargeAssignment extends ChargeAssignment {

    private final Long roleId;

    private RoleChargeAssignment(
            Long id,
            Long chargeDefinitionId,
            Long roleId,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        super(id, chargeDefinitionId, active, createdAt, updatedAt);
        this.roleId = validateId(roleId, "roleId");
    }

    public static RoleChargeAssignment create(
            Long chargeDefinitionId,
            Long roleId,
            Instant now
    ) {
        return new RoleChargeAssignment(
                null,
                chargeDefinitionId,
                roleId,
                true,
                now,
                null
        );
    }

    public static RoleChargeAssignment reconstitute(
            Long id,
            Long chargeDefinitionId,
            Long roleId,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new RoleChargeAssignment(
                id,
                chargeDefinitionId,
                roleId,
                active,
                createdAt,
                updatedAt
        );
    }

    @Override
    public ChargeAudienceType audienceType() {
        return ChargeAudienceType.ROLE;
    }
}