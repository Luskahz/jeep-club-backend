package com.jeepclub.backend.authentication.infra.persistence.mapper;

import com.jeepclub.backend.authentication.core.domain.model.RefreshToken;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.infra.persistence.entities.RefreshTokenEntity;

public class RefreshTokenMapper {

    private RefreshTokenMapper() {}

    public static RefreshToken toDomain(RefreshTokenEntity entity, Session session) {
        if (entity == null) return null;
        return RefreshToken.reconstitute(
                entity.getId(),
                session,
                entity.getTokenHash(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getStatus(),
                entity.getReplacedByTokenId()
        );
    }

    public static RefreshTokenEntity toEntity(RefreshToken token) {
        if (token == null) return null;
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setId(token.getId());
        entity.setSessionId(token.getSession().getId());
        entity.setTokenHash(token.getTokenHash());
        entity.setCreatedAt(token.getCreatedAt());
        entity.setExpiresAt(token.getExpiresAt());
        entity.setStatus(token.getStatus());
        entity.setReplacedByTokenId(token.getReplacedByTokenId());
        return entity;
    }
}