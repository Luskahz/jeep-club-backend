package com.jeepclub.backend.medical.core.domain;

import com.jeepclub.backend.medical.core.domain.exceptions.InvalidMedicalProfileException;

import java.time.Instant;

public class MedicalProfile {

    private final Long id;
    private final MedicalProfileOwnerType ownerType;
    private final Long ownerId;
    private BloodType bloodType;
    private String allergies;
    private String chronicConditions;
    private String continuousMedications;
    private String healthInsuranceProvider;
    private String healthInsurancePlan;
    private String healthInsuranceNumber;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;
    private String observations;
    private final Instant createdAt;
    private Instant updatedAt;

    private MedicalProfile(
            Long id,
            MedicalProfileOwnerType ownerType,
            Long ownerId,
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
            String observations,
            Instant createdAt,
            Instant updatedAt
    ) {
        validateOwner(ownerType, ownerId);
        validateDates(createdAt, updatedAt);

        this.id = id;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.bloodType = bloodType == null ? BloodType.UNKNOWN : bloodType;
        this.allergies = allergies;
        this.chronicConditions = chronicConditions;
        this.continuousMedications = continuousMedications;
        this.healthInsuranceProvider = healthInsuranceProvider;
        this.healthInsurancePlan = healthInsurancePlan;
        this.healthInsuranceNumber = healthInsuranceNumber;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactPhone = emergencyContactPhone;
        this.emergencyContactRelationship = emergencyContactRelationship;
        this.observations = observations;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static MedicalProfile create(
            MedicalProfileOwnerType ownerType,
            Long ownerId,
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
        Instant now = Instant.now();

        return new MedicalProfile(
                null,
                ownerType,
                ownerId,
                bloodType,
                allergies,
                chronicConditions,
                continuousMedications,
                healthInsuranceProvider,
                healthInsurancePlan,
                healthInsuranceNumber,
                emergencyContactName,
                emergencyContactPhone,
                emergencyContactRelationship,
                observations,
                now,
                now
        );
    }

    public static MedicalProfile reconstitute(
            Long id,
            MedicalProfileOwnerType ownerType,
            Long ownerId,
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
            String observations,
            Instant createdAt,
            Instant updatedAt
    ) {
        if (id == null) {
            throw new InvalidMedicalProfileException("O ID do perfil médico reconstituído é obrigatório.");
        }

        return new MedicalProfile(
                id,
                ownerType,
                ownerId,
                bloodType,
                allergies,
                chronicConditions,
                continuousMedications,
                healthInsuranceProvider,
                healthInsurancePlan,
                healthInsuranceNumber,
                emergencyContactName,
                emergencyContactPhone,
                emergencyContactRelationship,
                observations,
                createdAt,
                updatedAt
        );
    }

    public void update(
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
        this.bloodType = bloodType == null ? BloodType.UNKNOWN : bloodType;
        this.allergies = allergies;
        this.chronicConditions = chronicConditions;
        this.continuousMedications = continuousMedications;
        this.healthInsuranceProvider = healthInsuranceProvider;
        this.healthInsurancePlan = healthInsurancePlan;
        this.healthInsuranceNumber = healthInsuranceNumber;
        this.emergencyContactName = emergencyContactName;
        this.emergencyContactPhone = emergencyContactPhone;
        this.emergencyContactRelationship = emergencyContactRelationship;
        this.observations = observations;
        this.updatedAt = Instant.now();
    }

    private static void validateOwner(MedicalProfileOwnerType ownerType, Long ownerId) {
        if (ownerType == null) {
            throw new InvalidMedicalProfileException("O tipo do proprietário do perfil médico é obrigatório.");
        }

        if (ownerId == null) {
            throw new InvalidMedicalProfileException("O identificador do proprietário do perfil médico é obrigatório.");
        }
    }

    private static void validateDates(Instant createdAt, Instant updatedAt) {
        if (createdAt == null) {
            throw new InvalidMedicalProfileException("A data de criação do perfil médico é obrigatória.");
        }

        if (updatedAt == null) {
            throw new InvalidMedicalProfileException("A data de atualização do perfil médico é obrigatória.");
        }
    }

    public Long getId() {
        return id;
    }

    public MedicalProfileOwnerType getOwnerType() {
        return ownerType;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public BloodType getBloodType() {
        return bloodType;
    }

    public String getAllergies() {
        return allergies;
    }

    public String getChronicConditions() {
        return chronicConditions;
    }

    public String getContinuousMedications() {
        return continuousMedications;
    }

    public String getHealthInsuranceProvider() {
        return healthInsuranceProvider;
    }

    public String getHealthInsurancePlan() {
        return healthInsurancePlan;
    }

    public String getHealthInsuranceNumber() {
        return healthInsuranceNumber;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public String getEmergencyContactPhone() {
        return emergencyContactPhone;
    }

    public String getEmergencyContactRelationship() {
        return emergencyContactRelationship;
    }

    public String getObservations() {
        return observations;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
