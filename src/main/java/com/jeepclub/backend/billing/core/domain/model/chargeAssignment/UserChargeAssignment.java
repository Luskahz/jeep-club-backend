package com.jeepclub.backend.billing.core.domain.model.chargeAssignment;

import com.jeepclub.backend.billing.core.domain.enums.ChargeAudienceType;
import lombok.Getter;

import java.time.Instant;

@Getter
public final class UserChargeAssignment extends ChargeAssignment {

    private final Long userId;

    private UserChargeAssignment(
            Long id,
            Long chargeDefinitionId,
            Long userId,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        super(id, chargeDefinitionId, active, createdAt, updatedAt);
        this.userId = validateId(userId, "userId");
    }

    public static UserChargeAssignment create(
            Long chargeDefinitionId,
            Long userId,
            Instant now
    ) {
        return new UserChargeAssignment(
                null,
                chargeDefinitionId,
                userId,
                true,
                now,
                null
        );
    }

    public static UserChargeAssignment reconstitute(
            Long id,
            Long chargeDefinitionId,
            Long userId,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new UserChargeAssignment(
                id,
                chargeDefinitionId,
                userId,
                active,
                createdAt,
                updatedAt
        );
    }

    @Override
    public ChargeAudienceType audienceType() {
        return ChargeAudienceType.USER;
    }
}