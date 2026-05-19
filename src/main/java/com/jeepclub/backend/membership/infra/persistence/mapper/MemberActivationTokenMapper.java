package com.jeepclub.backend.membership.infra.persistence.mapper;

import com.jeepclub.backend.membership.core.domain.model.MemberActivationToken;
import com.jeepclub.backend.membership.infra.persistence.entity.MemberActivationTokenEntity;
import org.springframework.stereotype.Component;

@Component
public class MemberActivationTokenMapper {

    public MemberActivationToken toDomain(MemberActivationTokenEntity entity) {
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

    public MemberActivationTokenEntity toEntity(MemberActivationToken token) {
        if (token == null) return null;
        MemberActivationTokenEntity entity = new MemberActivationTokenEntity();
        entity.setId(token.getId());
        entity.setApplicationId(token.getApplicationId());
        entity.setTokenHash(token.getTokenHash());
        entity.setExpiresAt(token.getExpiresAt());
        entity.setUsedAt(token.getUsedAt());
        entity.setCreatedAt(token.getCreatedAt());
        return entity;
    }
}