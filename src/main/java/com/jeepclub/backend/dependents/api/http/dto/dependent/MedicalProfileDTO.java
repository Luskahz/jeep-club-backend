package com.jeepclub.backend.dependents.api.http.dto.dependent;

import com.jeepclub.backend.dependents.core.domain.model.MedicalProfile;
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
                profile.getBloodType(),
                profile.getAllergies(),
                profile.getChronicDiseases(),
                profile.getMedications(),
                profile.getMedicalNotes()
        );
    }

    public MedicalProfile toDomain() {
        return new MedicalProfile(
                bloodType,
                allergies,
                chronicDiseases,
                medications,
                medicalNotes
        );
    }
}

