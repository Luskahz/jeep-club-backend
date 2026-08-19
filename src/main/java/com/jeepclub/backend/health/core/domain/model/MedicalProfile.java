package com.jeepclub.backend.health.core.domain.model;

import com.jeepclub.backend.health.core.domain.enums.BloodType;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.exception.InvalidMedicalProfileException;
import lombok.Getter;

import java.time.Instant;

@Getter
public class MedicalProfile {

    private static final int MAX_HEALTH_INSURANCE_PROVIDER_LENGTH = 120;
    private static final int MAX_HEALTH_INSURANCE_PLAN_LENGTH = 120;
    private static final int MAX_HEALTH_INSURANCE_NUMBER_LENGTH = 80;
    private static final int MAX_EMERGENCY_CONTACT_NAME_LENGTH = 120;
    private static final int MAX_EMERGENCY_CONTACT_RELATIONSHIP_LENGTH = 80;

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
        this.bloodType = bloodType == null
                ? BloodType.UNKNOWN
                : bloodType;

        this.allergies = normalizeNullableText(allergies);
        this.chronicConditions = normalizeNullableText(chronicConditions);
        this.continuousMedications = normalizeNullableText(continuousMedications);

        this.healthInsuranceProvider = normalizeLimitedText(
                healthInsuranceProvider,
                MAX_HEALTH_INSURANCE_PROVIDER_LENGTH,
                "healthInsuranceProvider"
        );

        this.healthInsurancePlan = normalizeLimitedText(
                healthInsurancePlan,
                MAX_HEALTH_INSURANCE_PLAN_LENGTH,
                "healthInsurancePlan"
        );

        this.healthInsuranceNumber = normalizeLimitedText(
                healthInsuranceNumber,
                MAX_HEALTH_INSURANCE_NUMBER_LENGTH,
                "healthInsuranceNumber"
        );

        this.emergencyContactName = normalizeLimitedText(
                emergencyContactName,
                MAX_EMERGENCY_CONTACT_NAME_LENGTH,
                "emergencyContactName"
        );

        this.emergencyContactPhone = normalizePhoneNumber(
                emergencyContactPhone
        );

        this.emergencyContactRelationship = normalizeLimitedText(
                emergencyContactRelationship,
                MAX_EMERGENCY_CONTACT_RELATIONSHIP_LENGTH,
                "emergencyContactRelationship"
        );

        this.observations = normalizeNullableText(observations);
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

    private static String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();

        return normalized.isBlank()
                ? null
                : normalized;
    }

    private static String normalizeLimitedText(
            String value,
            int maxLength,
            String fieldName
    ) {
        String normalized = normalizeNullableText(value);

        if (normalized != null && normalized.length() > maxLength) {
            throw new InvalidMedicalProfileException(
                    fieldName + " não pode exceder "
                            + maxLength + " caracteres."
            );
        }

        return normalized;
    }

    private static String normalizePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return null;
        }

        String normalized = phoneNumber.replaceAll("\\D", "");

        if (normalized.length() < 10 || normalized.length() > 11) {
            throw new InvalidMedicalProfileException(
                    "O telefone de emergência deve possuir 10 ou 11 dígitos."
            );
        }

        return normalized;
    }
}