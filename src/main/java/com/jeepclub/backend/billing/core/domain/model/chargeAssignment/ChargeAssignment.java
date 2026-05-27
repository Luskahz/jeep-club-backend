package com.jeepclub.backend.billing.core.domain.model.chargeAssignment;

import com.jeepclub.backend.billing.core.domain.enums.ChargeAudienceType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ChargeAssignment {

    private Long id;
    private Long chargeDefinitionId;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    protected ChargeAssignment(
            Long id,
            Long chargeDefinitionId,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.chargeDefinitionId = validateId(chargeDefinitionId, "chargeDefinitionId");
        this.active = active;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = updatedAt;
    }

    public abstract ChargeAudienceType audienceType();

    public void activate(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        if (active) {
            throw new IllegalStateException("Charge assignment is already active.");
        }

        this.active = true;
        this.updatedAt = now;
    }

    public void deactivate(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        if (!active) {
            throw new IllegalStateException("Charge assignment is already inactive.");
        }

        this.active = false;
        this.updatedAt = now;
    }

    protected static Long validateId(Long id, String fieldName) {
        Objects.requireNonNull(id, fieldName + " cannot be null");

        if (id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero.");
        }

        return id;
    }
}