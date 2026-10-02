package com.jeepclub.backend.memberships.infra.persistence.mapper;

import com.jeepclub.backend.memberships.core.domain.model.MembershipBillingConfiguration;
import com.jeepclub.backend.memberships.infra.persistence.entity.MembershipBillingConfigurationEntity;

public final class MembershipBillingConfigurationMapper {

    private MembershipBillingConfigurationMapper() {}

    public static MembershipBillingConfiguration toDomain(MembershipBillingConfigurationEntity entity) {
        return MembershipBillingConfiguration.reconstitute(
                entity.getId(),
                entity.getChargeDefinitionId(),
                entity.isEnforcementEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public static MembershipBillingConfigurationEntity toEntity(
            MembershipBillingConfiguration configuration
    ) {
        MembershipBillingConfigurationEntity entity = new MembershipBillingConfigurationEntity();
        entity.setId(configuration.getId());
        entity.setChargeDefinitionId(configuration.getChargeDefinitionId());
        entity.setEnforcementEnabled(configuration.isEnforcementEnabled());
        entity.setCreatedAt(configuration.getCreatedAt());
        entity.setUpdatedAt(configuration.getUpdatedAt());
        return entity;
    }
}
