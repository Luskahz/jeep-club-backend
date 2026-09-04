package com.jeepclub.backend.iam.authentication.infra.persistence.mapper;

import com.jeepclub.backend.iam.authentication.core.domain.model.PasswordRecoveryRequest;
import com.jeepclub.backend.iam.authentication.infra.persistence.entity.PasswordRecoveryRequestEntity;

import java.util.Objects;

public final class PasswordRecoveryRequestMapper {

    private PasswordRecoveryRequestMapper() {
    }

    public static PasswordRecoveryRequest toDomain(
            PasswordRecoveryRequestEntity entity
    ) {
        Objects.requireNonNull(entity, "entity cannot be null");

        return PasswordRecoveryRequest.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getExpiresAt(),
                entity.getCreatedAt(),
                entity.getResolvedAt(),
                entity.getCancelledAt(),
                entity.getStatus(),
                entity.getMethod()
        );
    }

    public static PasswordRecoveryRequestEntity toEntity(
            PasswordRecoveryRequest request
    ) {
        Objects.requireNonNull(request, "request cannot be null");

        PasswordRecoveryRequestEntity entity = new PasswordRecoveryRequestEntity();

        entity.setId(request.getId());
        entity.setUserId(request.getUserId());
        entity.setTokenHash(request.getTokenHash());
        entity.setCreatedAt(request.getCreatedAt());
        entity.setExpiresAt(request.getExpiresAt());
        entity.setResolvedAt(request.getResolvedAt());
        entity.setCancelledAt(request.getCancelledAt());
        entity.setStatus(request.getStatus());
        entity.setMethod(request.getMethod());

        return entity;
    }
}