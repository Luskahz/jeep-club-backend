package com.jeepclub.backend.health.api.module.medicalprofile;
import java.util.Optional;
public interface EmergencyMedicalProfileQuery {
    Optional<Profile> find(MedicalProfileOwner type, Long ownerId);
    record Profile(String bloodType, String allergies, String chronicConditions, String continuousMedications,
        String healthInsuranceProvider, String healthInsurancePlan, String healthInsuranceNumber,
        String emergencyContactName, String emergencyContactPhone, String emergencyContactRelationship, String observations) {}
}
