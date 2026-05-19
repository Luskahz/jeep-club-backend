package com.jeepclub.backend.medical.api.dto;

import com.jeepclub.backend.medical.core.domain.BloodType;
import jakarta.validation.constraints.Size;

public record MedicalProfileRequest(

        BloodType bloodType,

        @Size(max = 2000, message = "As alergias devem ter no máximo 2000 caracteres.")
        String allergies,

        @Size(max = 2000, message = "As doenças/condições devem ter no máximo 2000 caracteres.")
        String chronicConditions,

        @Size(max = 2000, message = "Os medicamentos devem ter no máximo 2000 caracteres.")
        String continuousMedications,

        @Size(max = 120, message = "O nome do convênio deve ter no máximo 120 caracteres.")
        String healthInsuranceProvider,

        @Size(max = 120, message = "O plano do convênio deve ter no máximo 120 caracteres.")
        String healthInsurancePlan,

        @Size(max = 80, message = "O número do convênio deve ter no máximo 80 caracteres.")
        String healthInsuranceNumber,

        @Size(max = 120, message = "O nome do contato de emergência deve ter no máximo 120 caracteres.")
        String emergencyContactName,

        @Size(max = 20, message = "O telefone do contato de emergência deve ter no máximo 20 caracteres.")
        String emergencyContactPhone,

        @Size(max = 80, message = "O parentesco/relação deve ter no máximo 80 caracteres.")
        String emergencyContactRelationship,

        @Size(max = 2000, message = "As observações devem ter no máximo 2000 caracteres.")
        String observations
) {
}