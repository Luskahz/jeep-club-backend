package com.jeepclub.backend.authentication.infra.persistence.mapper;

import com.jeepclub.backend.authentication.core.domain.model.PasswordResetRequest;
import com.jeepclub.backend.authentication.infra.persistence.entity.PasswordResetRequestEntity;

public final class PasswordResetRequestMapper {

    private PasswordResetRequestMapper() {
    }

    public static PasswordResetRequestEntity toEntity(PasswordResetRequest domain) {
        PasswordResetRequestEntity entity = new PasswordResetRequestEntity();

        if (domain.getId() != null) {
            entity.setId(domain.getId());
        }

        entity.setUserId(domain.getUserId());
        entity.setTokenHash(domain.getTokenHash());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setExpiresAt(domain.getExpiresAt());
        entity.setUsedAt(domain.getUsedAt());
        entity.setStatus(domain.getStatus());

        return entity;
    }

    public static PasswordResetRequest toDomain(PasswordResetRequestEntity entity) {
        return PasswordResetRequest.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getUsedAt(),
                entity.getStatus()
        );
    }
}