package com.jeepclub.backend.memberships.infra.persistence.mapper;

import com.jeepclub.backend.memberships.core.domain.model.MemberActivationToken;
import com.jeepclub.backend.memberships.infra.persistence.entity.MemberActivationTokenEntity;

public class MemberActivationTokenMapper {

    private MemberActivationTokenMapper() {}

    public static MemberActivationToken toDomain(MemberActivationTokenEntity entity) {
        if (entity == null) return null;
        return MemberActivationToken.reconstitute(
                entity.getId(),
                entity.getApplicationId(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getUsedAt(),
                entity.getCreatedAt()
        );
    }

    public static MemberActivationTokenEntity toEntity(MemberActivationToken domain) {
        if (domain == null) return null;
        MemberActivationTokenEntity entity = new MemberActivationTokenEntity();
        entity.setId(domain.getId());
        entity.setApplicationId(domain.getApplicationId());
        entity.setTokenHash(domain.getTokenHash());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setUsedAt(domain.getUsedAt());
        entity.setCreatedAt(domain.getCreatedAt());
        return entity;
    }
}