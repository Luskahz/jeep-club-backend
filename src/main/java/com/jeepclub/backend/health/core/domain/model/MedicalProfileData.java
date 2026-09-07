package com.jeepclub.backend.health.core.domain.model;

import com.jeepclub.backend.health.core.domain.enums.BloodType;
import com.jeepclub.backend.health.core.domain.exception.InvalidMedicalProfileException;

/**
 * Single source of truth for medical profile data normalization and limits.
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

    public static final int MAX_LONG_TEXT_LENGTH = 2000;
    public static final int MAX_HEALTH_INSURANCE_PROVIDER_LENGTH = 120;
    public static final int MAX_HEALTH_INSURANCE_PLAN_LENGTH = 120;
    public static final int MAX_HEALTH_INSURANCE_NUMBER_LENGTH = 80;
    public static final int MAX_EMERGENCY_CONTACT_NAME_LENGTH = 120;
    public static final int MAX_EMERGENCY_CONTACT_RELATIONSHIP_LENGTH = 80;

    public MedicalProfileData {
        bloodType = bloodType == null ? BloodType.UNKNOWN : bloodType;
        allergies = normalizeLimitedText(
                allergies,
                MAX_LONG_TEXT_LENGTH,
                "allergies"
        );
        chronicConditions = normalizeLimitedText(
                chronicConditions,
                MAX_LONG_TEXT_LENGTH,
                "chronicConditions"
        );
        continuousMedications = normalizeLimitedText(
                continuousMedications,
                MAX_LONG_TEXT_LENGTH,
                "continuousMedications"
        );
        healthInsuranceProvider = normalizeLimitedText(
                healthInsuranceProvider,
                MAX_HEALTH_INSURANCE_PROVIDER_LENGTH,
                "healthInsuranceProvider"
        );
        healthInsurancePlan = normalizeLimitedText(
                healthInsurancePlan,
                MAX_HEALTH_INSURANCE_PLAN_LENGTH,
                "healthInsurancePlan"
        );
        healthInsuranceNumber = normalizeLimitedText(
                healthInsuranceNumber,
                MAX_HEALTH_INSURANCE_NUMBER_LENGTH,
                "healthInsuranceNumber"
        );
        emergencyContactName = normalizeLimitedText(
                emergencyContactName,
                MAX_EMERGENCY_CONTACT_NAME_LENGTH,
                "emergencyContactName"
        );
        emergencyContactPhone = normalizePhone(emergencyContactPhone);
        emergencyContactRelationship = normalizeLimitedText(
                emergencyContactRelationship,
                MAX_EMERGENCY_CONTACT_RELATIONSHIP_LENGTH,
                "emergencyContactRelationship"
        );
        observations = normalizeLimitedText(
                observations,
                MAX_LONG_TEXT_LENGTH,
                "observations"
        );
    }

    private static String normalizeLimitedText(
            String value,
            int maxLength,
            String fieldName
    ) {
        String normalized = normalizeNullableText(value);
        if (normalized != null && normalized.length() > maxLength) {
            throw new InvalidMedicalProfileException(
                    fieldName + " não pode exceder " + maxLength + " caracteres."
            );
        }
        return normalized;
    }

    private static String normalizeNullableText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private static String normalizePhone(String value) {
        String normalizedText = normalizeNullableText(value);
        if (normalizedText == null) {
            return null;
        }

        String digits = normalizedText.replaceAll("\\D", "");
        if (digits.length() < 10 || digits.length() > 11) {
            throw new InvalidMedicalProfileException(
                    "O telefone de emergência deve possuir 10 ou 11 dígitos."
            );
        }
        return digits;
    }

    @Override
    public String toString() {
        return "MedicalProfileData[REDACTED]";
    }
}
