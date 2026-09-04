package com.jeepclub.backend.identity.infra.persistence.mapper;

import com.jeepclub.backend.identity.core.domain.model.User;
import com.jeepclub.backend.identity.infra.persistence.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserEntity toEntity(User domain) {
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
        entity.setPhoneNumber(domain.getPhoneNumber());
        entity.setProfilePhotoUrl(domain.getProfilePhotoUrl());
        entity.setStatus(domain.getStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setDisabledAt(domain.getDisabledAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        return entity;
    }

    public User toDomain(UserEntity entity) {
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
                entity.getPhoneNumber(),
                entity.getProfilePhotoUrl(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getDisabledAt(),
                entity.getUpdatedAt()
        );
    }
}
