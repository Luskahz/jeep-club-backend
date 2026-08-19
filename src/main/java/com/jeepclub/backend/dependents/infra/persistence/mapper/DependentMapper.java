package com.jeepclub.backend.dependents.infra.persistence.mapper;

import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.dependents.infra.persistence.entity.DependentEntity;
import org.springframework.stereotype.Component;

@Component
public class DependentMapper {

    public DependentEntity toEntity(Dependent domain) {
        if (domain == null) {
            return null;
        }
        DependentEntity entity = new DependentEntity();

        entity.setId(domain.getId());
        entity.setName(domain.getName());
        entity.setCpf(domain.getCpf());
        entity.setBirthDate(domain.getBirthDate());
        entity.setRelationshipType(domain.getRelationshipType());
        entity.setPhoneNumber(domain.getPhoneNumber());
        entity.setUserId(domain.getUserId());
        entity.setStatus(domain.getStatus());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setDeletedAt(domain.getDeletedAt());

        return entity;
    }

    public Dependent toDomain(DependentEntity entity) {
        if (entity == null) {
            return null;
        }

        return Dependent.reconstitute(
                entity.getId(),
                entity.getName(),
                entity.getCpf(),
                entity.getBirthDate(),
                entity.getRelationshipType(),
                entity.getPhoneNumber(),
                entity.getUserId(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getDeletedAt()
        );
    }
}
