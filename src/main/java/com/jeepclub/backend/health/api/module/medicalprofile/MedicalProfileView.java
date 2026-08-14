package com.jeepclub.backend.health.api.module.medicalprofile;

import java.time.Instant;

public record MedicalProfileView(
        Long id,
        MedicalProfileOwner ownerType,
        Long ownerId,
        MedicalProfileBloodType bloodType,
        String allergies,
        String chronicConditions,
        String continuousMedications,
        String healthInsuranceProvider,
        String healthInsurancePlan,
        String healthInsuranceNumber,
        String emergencyContactName,
        String emergencyContactPhone,
        String emergencyContactRelationship,
        String observations,
        Instant createdAt,
        Instant updatedAt
) {
}
