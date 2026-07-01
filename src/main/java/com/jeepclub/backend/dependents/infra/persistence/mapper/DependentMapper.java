package com.jeepclub.backend.dependents.infra.persistence.mapper;

import com.jeepclub.backend.authentication.infra.persistence.entity.UserEntity;
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

        entity.setConsentAccepted(domain.isConsentAccepted());
        entity.setConsentAcceptedAt(domain.getConsentAcceptedAt());

        // Setting lazy-loaded User reference
        if (domain.getSocioId() != null) {
            UserEntity socio = new UserEntity();
            socio.setId(domain.getSocioId());
            entity.setSocio(socio);
        }

        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        return entity;
    }

    public Dependent toDomain(DependentEntity entity) {
        if (entity == null) {
            return null;
        }

        Long socioId = (entity.getSocio() != null) ? entity.getSocio().getId() : null;

        return Dependent.reconstitute(
                entity.getId(),
                entity.getName(),
                entity.getCpf(),
                entity.getBirthDate(),
                entity.getRelationshipType(),
                entity.getPhoneNumber(),
                entity.isConsentAccepted(),
                entity.getConsentAcceptedAt(),
                socioId,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
