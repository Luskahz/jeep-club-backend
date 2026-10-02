package com.jeepclub.backend.memberships.core.domain.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MembershipBillingConfiguration {

    private Long id;
    private Long chargeDefinitionId;
    private boolean enforcementEnabled;
    private Instant createdAt;
    private Instant updatedAt;

    public static MembershipBillingConfiguration create(
            Long chargeDefinitionId,
            boolean enforcementEnabled,
            Instant now
    ) {
        Objects.requireNonNull(now, "now must not be null");

        MembershipBillingConfiguration configuration = new MembershipBillingConfiguration();
        configuration.chargeDefinitionId = validateId(chargeDefinitionId);
        configuration.enforcementEnabled = enforcementEnabled;
        configuration.createdAt = now;
        configuration.updatedAt = now;
        return configuration;
    }

    public static MembershipBillingConfiguration reconstitute(
            Long id,
            Long chargeDefinitionId,
            boolean enforcementEnabled,
            Instant createdAt,
            Instant updatedAt
    ) {
        MembershipBillingConfiguration configuration = new MembershipBillingConfiguration();
        configuration.id = validateId(id);
        configuration.chargeDefinitionId = validateId(chargeDefinitionId);
        configuration.enforcementEnabled = enforcementEnabled;
        configuration.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        configuration.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        return configuration;
    }

    public void replaceChargeDefinition(Long chargeDefinitionId, Instant now) {
        this.chargeDefinitionId = validateId(chargeDefinitionId);
        this.updatedAt = Objects.requireNonNull(now, "now must not be null");
    }

    public void setEnforcementEnabled(boolean enforcementEnabled, Instant now) {
        this.enforcementEnabled = enforcementEnabled;
        this.updatedAt = Objects.requireNonNull(now, "now must not be null");
    }

    private static Long validateId(Long id) {
        Objects.requireNonNull(id, "id must not be null");
        if (id <= 0) {
            throw new IllegalArgumentException("id must be greater than zero");
        }
        return id;
    }
}
