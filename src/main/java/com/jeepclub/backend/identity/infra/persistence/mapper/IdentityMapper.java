package com.jeepclub.backend.identity.infra.persistence.mapper;

import com.jeepclub.backend.identity.core.domain.model.Identity;
import com.jeepclub.backend.identity.infra.persistence.entity.IdentityEntity;
import org.springframework.stereotype.Component;

@Component
public class IdentityMapper {

    public IdentityEntity toEntity(Identity domain) {
        if (domain == null) {
            return null;
        }

        IdentityEntity entity = new IdentityEntity();
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

    public Identity toDomain(IdentityEntity entity) {
        if (entity == null) {
            return null;
        }

        return Identity.reconstitute(
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
