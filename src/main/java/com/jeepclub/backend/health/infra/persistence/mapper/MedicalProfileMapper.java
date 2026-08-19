package com.jeepclub.backend.health.infra.persistence.mapper;

import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import com.jeepclub.backend.health.infra.persistence.entity.MedicalProfileEntity;
import org.springframework.stereotype.Component;

@Component
public class MedicalProfileMapper {

    public MedicalProfileEntity toEntity(MedicalProfile domain) {
        if (domain == null) {
            return null;
        }

        MedicalProfileEntity entity = new MedicalProfileEntity();

        entity.setId(domain.getId());
        entity.setOwnerType(domain.getOwnerType());
        entity.setOwnerId(domain.getOwnerId());
        entity.setBloodType(domain.getBloodType());
        entity.setAllergies(domain.getAllergies());
        entity.setChronicConditions(domain.getChronicConditions());
        entity.setContinuousMedications(domain.getContinuousMedications());
        entity.setHealthInsuranceProvider(domain.getHealthInsuranceProvider());
        entity.setHealthInsurancePlan(domain.getHealthInsurancePlan());
        entity.setHealthInsuranceNumber(domain.getHealthInsuranceNumber());
        entity.setEmergencyContactName(domain.getEmergencyContactName());
        entity.setEmergencyContactPhone(domain.getEmergencyContactPhone());
        entity.setEmergencyContactRelationship(domain.getEmergencyContactRelationship());
        entity.setObservations(domain.getObservations());
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());

        return entity;
    }

    public MedicalProfile toDomain(MedicalProfileEntity entity) {
        if (entity == null) {
            return null;
        }

        return MedicalProfile.reconstitute(
                entity.getId(),
                entity.getOwnerType(),
                entity.getOwnerId(),
                entity.getBloodType(),
                entity.getAllergies(),
                entity.getChronicConditions(),
                entity.getContinuousMedications(),
                entity.getHealthInsuranceProvider(),
                entity.getHealthInsurancePlan(),
                entity.getHealthInsuranceNumber(),
                entity.getEmergencyContactName(),
                entity.getEmergencyContactPhone(),
                entity.getEmergencyContactRelationship(),
                entity.getObservations(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}