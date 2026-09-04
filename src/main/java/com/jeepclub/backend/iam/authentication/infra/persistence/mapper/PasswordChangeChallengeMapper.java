package com.jeepclub.backend.iam.authentication.infra.persistence.mapper;

import com.jeepclub.backend.iam.authentication.core.domain.model.PasswordChangeChallenge;
import com.jeepclub.backend.iam.authentication.infra.persistence.entity.PasswordChangeChallengeEntity;

import java.util.Objects;

public final class PasswordChangeChallengeMapper {

    private PasswordChangeChallengeMapper() {
    }

    public static PasswordChangeChallenge toDomain(
            PasswordChangeChallengeEntity entity
    ) {
        Objects.requireNonNull(entity, "entity cannot be null");

        return PasswordChangeChallenge.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getUsedAt(),
                entity.isUsed()
        );
    }

    public static PasswordChangeChallengeEntity toEntity(
            PasswordChangeChallenge challenge
    ) {
        Objects.requireNonNull(challenge, "challenge cannot be null");

        PasswordChangeChallengeEntity entity = new PasswordChangeChallengeEntity();

        entity.setId(challenge.getId());
        entity.setUserId(challenge.getUserId());
        entity.setTokenHash(challenge.getTokenHash());
        entity.setCreatedAt(challenge.getCreatedAt());
        entity.setExpiresAt(challenge.getExpiresAt());
        entity.setUsedAt(challenge.getUsedAt());
        entity.setUsed(challenge.isUsed());

        return entity;
    }
}