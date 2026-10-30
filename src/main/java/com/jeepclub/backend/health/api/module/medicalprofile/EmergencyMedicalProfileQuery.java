package com.jeepclub.backend.health.api.module.medicalprofile;
import java.util.Optional;
public interface EmergencyMedicalProfileQuery {
    /** Caller must establish confirmed Event participation and emergency permission.
     * Rechecks current ownership; availability does not revoke an existing participation. */
    Optional<Profile> findConfirmedDependent(Long dependentId,Long responsibleUserId);
    Optional<Profile> find(MedicalProfileOwner type, Long ownerId);
    record Profile(String bloodType, String allergies, String chronicConditions, String continuousMedications,
        String healthInsuranceProvider, String healthInsurancePlan, String healthInsuranceNumber,
        String emergencyContactName, String emergencyContactPhone, String emergencyContactRelationship, String observations) {}
}
