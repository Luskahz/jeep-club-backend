package com.jeepclub.backend.health.core.application;

import com.jeepclub.backend.health.core.domain.enums.BloodType;

/**
 * Framework-neutral input consumed by the Health application layer.
 */
public record MedicalProfileData(
        BloodType bloodType,
        String allergies,
        String chronicConditions,
        String continuousMedications,
        String healthInsuranceProvider,
        String healthInsurancePlan,
        String healthInsuranceNumber,
        String emergencyContactName,
        String emergencyContactPhone,
        String emergencyContactRelationship,
        String observations
) {
}
