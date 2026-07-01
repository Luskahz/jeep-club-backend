package com.jeepclub.backend.dependents.api.http.dto.dependent;

import com.jeepclub.backend.health.api.http.dto.MedicalProfileRequest;
import com.jeepclub.backend.health.core.domain.BloodType;
import com.jeepclub.backend.health.core.domain.MedicalProfile;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Perfil médico do dependente contendo dados sensíveis.")
public record MedicalProfileDTO(
        @Schema(description = "Tipo sanguíneo.", example = "O+", allowableValues = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"})
        String bloodType,

        @Schema(description = "Alergias.", example = "Alergia a frutos do mar, dipirona")
        String allergies,

        @Schema(description = "Doenças crônicas.", example = "Hipertensão")
        String chronicDiseases,

        @Schema(description = "Medicamentos em uso contínuo.", example = "Losartana 50mg")
        String medications,

        @Schema(description = "Notas médicas gerais e recomendações.", example = "Usar óculos de grau")
        String medicalNotes
) {
    public static MedicalProfileDTO from(MedicalProfile profile) {
        if (profile == null) {
            return new MedicalProfileDTO(null, null, null, null, null);
        }
        return new MedicalProfileDTO(
                profile.getBloodType() == null ? null : profile.getBloodType().name(),
                profile.getAllergies(),
                profile.getChronicConditions(),
                profile.getContinuousMedications(),
                profile.getObservations()
        );
    }

    public MedicalProfileRequest toHealthRequest() {
        return new MedicalProfileRequest(
                parseBloodType(bloodType),
                allergies,
                chronicDiseases,
                medications,
                null,
                null,
                null,
                null,
                null,
                null,
                medicalNotes
        );
    }

    public boolean hasAnyValue() {
        return hasText(bloodType)
                || hasText(allergies)
                || hasText(chronicDiseases)
                || hasText(medications)
                || hasText(medicalNotes);
    }

    private BloodType parseBloodType(String value) {
        if (!hasText(value)) {
            return BloodType.UNKNOWN;
        }

        String normalized = value.trim()
                .toUpperCase()
                .replace("+", "_POSITIVE")
                .replace("-", "_NEGATIVE");

        return BloodType.valueOf(normalized);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
