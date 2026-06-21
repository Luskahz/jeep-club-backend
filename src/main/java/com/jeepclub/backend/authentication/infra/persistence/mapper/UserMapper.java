package com.jeepclub.backend.authentication.infra.persistence.mapper;

import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.infra.persistence.entity.UserEntity;

public final class UserMapper {

    private UserMapper() {
    }

    public static User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }

        return User.reconstitute(
                entity.getId(),
                entity.getName(),
                entity.getBirthDate(),
                entity.getEmail(),
                entity.getCpf(),
                entity.getRg(),
                entity.getPasswordHash(),
                entity.getPhoneNumber(),
                entity.getProfilePhotoUrl(),
                entity.getStatus(),
                entity.getLastLoginAt(),
                entity.getCreatedAt(),
                entity.getDisabledAt(),
                entity.getUpdatedAt(),
                entity.getPasswordChangeAt(),
                entity.getFailedLoginAttempts()
        );
    }

    public static UserEntity toEntity(User domain) {
        if (domain == null) {
            return null;
        }

        UserEntity entity = new UserEntity();

        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setBirthDate(domain.getBirthDate());
        entity.setEmail(domain.getEmail());
        entity.setCpf(domain.getCpf());
        entity.setRg(domain.getRg());
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setPhoneNumber(domain.getPhoneNumber());
        entity.setProfilePhotoUrl(domain.getProfilePhotoUrl());
        entity.setStatus(domain.getStatus());
        entity.setLastLoginAt(domain.getLastLoginAt());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setDisabledAt(domain.getDisabledAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setPasswordChangeAt(
                domain.getPasswordChangedAt()
        );
        entity.setFailedLoginAttempts(
                domain.getFailedLoginAttempts()
        );

        return entity;
    }
}
