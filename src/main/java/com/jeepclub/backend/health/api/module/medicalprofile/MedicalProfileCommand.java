package com.jeepclub.backend.health.api.module.medicalprofile;

public record MedicalProfileCommand(
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
        String observations
) {
}
