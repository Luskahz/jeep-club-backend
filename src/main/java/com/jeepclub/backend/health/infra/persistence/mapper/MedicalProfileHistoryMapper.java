package com.jeepclub.backend.health.infra.persistence.mapper;

import com.jeepclub.backend.health.infra.persistence.entity.MedicalProfileEntity;
import com.jeepclub.backend.health.infra.persistence.entity.MedicalProfileHistoryEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class MedicalProfileHistoryMapper {

    public MedicalProfileHistoryEntity toHistoryEntity(
            MedicalProfileEntity source,
            Long deletedByUserId,
            Instant deletedAt
    ) {
        if (source == null) {
            return null;
        }

        MedicalProfileHistoryEntity history = new MedicalProfileHistoryEntity();
        history.setMedicalProfileId(source.getId());
        history.setOwnerType(source.getOwnerType());
        history.setOwnerId(source.getOwnerId());
        history.setBloodType(source.getBloodType());
        history.setAllergies(source.getAllergies());
        history.setChronicConditions(source.getChronicConditions());
        history.setContinuousMedications(source.getContinuousMedications());
        history.setHealthInsuranceProvider(source.getHealthInsuranceProvider());
        history.setHealthInsurancePlan(source.getHealthInsurancePlan());
        history.setHealthInsuranceNumber(source.getHealthInsuranceNumber());
        history.setEmergencyContactName(source.getEmergencyContactName());
        history.setEmergencyContactPhone(source.getEmergencyContactPhone());
        history.setEmergencyContactRelationship(source.getEmergencyContactRelationship());
        history.setObservations(source.getObservations());
        history.setDeletedByUserId(deletedByUserId);
        history.setCreatedAt(source.getCreatedAt());
        history.setUpdatedAt(source.getUpdatedAt());
        history.setDeletedAt(deletedAt);

        return history;
    }
}
