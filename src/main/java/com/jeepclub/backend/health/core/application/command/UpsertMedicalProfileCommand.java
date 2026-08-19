package com.jeepclub.backend.health.core.application.command;

import com.jeepclub.backend.health.core.domain.enums.BloodType;

public record UpsertMedicalProfileCommand(
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