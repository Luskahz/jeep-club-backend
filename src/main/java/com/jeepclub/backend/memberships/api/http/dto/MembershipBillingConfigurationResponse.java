package com.jeepclub.backend.memberships.api.http.dto;

import com.jeepclub.backend.memberships.core.domain.model.MembershipBillingConfiguration;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record MembershipBillingConfigurationResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "12") Long chargeDefinitionId,
        @Schema(example = "true") boolean enforcementEnabled,
        Instant createdAt,
        Instant updatedAt
) {
    public static MembershipBillingConfigurationResponse from(
            MembershipBillingConfiguration configuration
    ) {
        return new MembershipBillingConfigurationResponse(
                configuration.getId(),
                configuration.getChargeDefinitionId(),
                configuration.isEnforcementEnabled(),
                configuration.getCreatedAt(),
                configuration.getUpdatedAt()
        );
    }
}
