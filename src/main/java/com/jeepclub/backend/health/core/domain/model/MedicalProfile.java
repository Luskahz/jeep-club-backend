package com.jeepclub.backend.health.core.domain.model;

import com.jeepclub.backend.health.core.domain.enums.BloodType;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.exception.InvalidMedicalProfileException;
import lombok.Getter;

import java.time.Instant;

@Getter
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
        this.id = id;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;

        applyMedicalData(
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
                observations
        );
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
            String observations,
            Instant now
    ) {
        validateOwner(ownerType, ownerId);
        validateNow(now);

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
        validateId(id);
        validateOwner(ownerType, ownerId);
        validateDates(createdAt, updatedAt);

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
            String observations,
            Instant now
    ) {
        validateUpdateDate(now);

        applyMedicalData(
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
                observations
        );

        this.updatedAt = now;
    }

    private void applyMedicalData(
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
        MedicalProfileData normalized = new MedicalProfileData(
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
                observations
        );

        this.bloodType = normalized.bloodType();
        this.allergies = normalized.allergies();
        this.chronicConditions = normalized.chronicConditions();
        this.continuousMedications = normalized.continuousMedications();
        this.healthInsuranceProvider = normalized.healthInsuranceProvider();
        this.healthInsurancePlan = normalized.healthInsurancePlan();
        this.healthInsuranceNumber = normalized.healthInsuranceNumber();
        this.emergencyContactName = normalized.emergencyContactName();
        this.emergencyContactPhone = normalized.emergencyContactPhone();
        this.emergencyContactRelationship = normalized.emergencyContactRelationship();
        this.observations = normalized.observations();
    }

    private static void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new InvalidMedicalProfileException(
                    "O ID do perfil médico deve ser positivo."
            );
        }
    }

    private static void validateOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    ) {
        if (ownerType == null) {
            throw new InvalidMedicalProfileException(
                    "O tipo do proprietário do perfil médico é obrigatório."
            );
        }

        if (ownerId == null || ownerId <= 0) {
            throw new InvalidMedicalProfileException(
                    "O identificador do proprietário deve ser positivo."
            );
        }
    }

    private static void validateNow(Instant now) {
        if (now == null) {
            throw new InvalidMedicalProfileException(
                    "A data da operação é obrigatória."
            );
        }
    }

    private static void validateDates(
            Instant createdAt,
            Instant updatedAt
    ) {
        if (createdAt == null) {
            throw new InvalidMedicalProfileException(
                    "A data de criação do perfil médico é obrigatória."
            );
        }

        if (updatedAt == null) {
            throw new InvalidMedicalProfileException(
                    "A data de atualização do perfil médico é obrigatória."
            );
        }

        if (updatedAt.isBefore(createdAt)) {
            throw new InvalidMedicalProfileException(
                    "A data de atualização não pode ser anterior à criação."
            );
        }
    }

    private void validateUpdateDate(Instant now) {
        validateNow(now);

        if (now.isBefore(createdAt)) {
            throw new InvalidMedicalProfileException(
                    "A data de atualização não pode ser anterior à criação."
            );
        }

        if (now.isBefore(updatedAt)) {
            throw new InvalidMedicalProfileException(
                    "A data de atualização não pode regredir."
            );
        }
    }

}
