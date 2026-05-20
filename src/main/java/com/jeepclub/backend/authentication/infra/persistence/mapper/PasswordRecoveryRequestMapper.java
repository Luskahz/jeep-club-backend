package com.jeepclub.backend.authentication.infra.persistence.mapper;

import com.jeepclub.backend.authentication.core.domain.model.PasswordRecoveryRequest;
import com.jeepclub.backend.authentication.infra.persistence.entity.PasswordRecoveryRequestEntity;

public final class PasswordRecoveryRequestMapper {

    private PasswordRecoveryRequestMapper() {
    }

    public static PasswordRecoveryRequestEntity toEntity(PasswordRecoveryRequest domain) {
        PasswordRecoveryRequestEntity entity = new PasswordRecoveryRequestEntity();

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

    public static PasswordRecoveryRequest toDomain(PasswordRecoveryRequestEntity entity) {
        return PasswordRecoveryRequest.reconstitute(
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