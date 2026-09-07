package com.jeepclub.backend.health.api.http.dto;

import com.jeepclub.backend.health.core.application.command.UpsertMedicalProfileCommand;
import com.jeepclub.backend.health.core.domain.enums.BloodType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfileData;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados para criação ou atualização de um perfil médico.")
public record MedicalProfileRequest(

        @Schema(
                description = "Tipo sanguíneo do usuário ou dependente.",
                example = "O_POSITIVE"
        )
        BloodType bloodType,

        @Schema(
                description = "Alergias conhecidas.",
                example = "Dipirona, amendoim e picada de abelha.",
                maxLength = MedicalProfileData.MAX_LONG_TEXT_LENGTH
        )
        String allergies,

        @Schema(
                description = "Doenças ou condições crônicas relevantes.",
                example = "Asma e hipertensão.",
                maxLength = MedicalProfileData.MAX_LONG_TEXT_LENGTH
        )
        String chronicConditions,

        @Schema(
                description = "Medicamentos de uso contínuo.",
                example = "Losartana 50mg diariamente.",
                maxLength = MedicalProfileData.MAX_LONG_TEXT_LENGTH
        )
        String continuousMedications,

        @Schema(
                description = "Nome da operadora do convênio médico.",
                example = "Unimed",
                maxLength = MedicalProfileData.MAX_HEALTH_INSURANCE_PROVIDER_LENGTH
        )
        String healthInsuranceProvider,

        @Schema(
                description = "Nome ou categoria do plano de saúde.",
                example = "Enfermaria",
                maxLength = MedicalProfileData.MAX_HEALTH_INSURANCE_PLAN_LENGTH
        )
        String healthInsurancePlan,

        @Schema(
                description = "Número da carteirinha ou identificação do convênio.",
                example = "123456789",
                maxLength = MedicalProfileData.MAX_HEALTH_INSURANCE_NUMBER_LENGTH
        )
        String healthInsuranceNumber,

        @Schema(
                description = "Nome do contato de emergência.",
                example = "Maria da Silva",
                maxLength = MedicalProfileData.MAX_EMERGENCY_CONTACT_NAME_LENGTH
        )
        String emergencyContactName,

        @Schema(
                description = "Telefone do contato de emergência.",
                example = "(12) 99999-9999"
        )
        String emergencyContactPhone,

        @Schema(
                description = "Relação do contato de emergência com o usuário ou dependente.",
                example = "Mãe",
                maxLength = MedicalProfileData.MAX_EMERGENCY_CONTACT_RELATIONSHIP_LENGTH
        )
        String emergencyContactRelationship,

        @Schema(
                description = "Observações médicas gerais.",
                example = "Em caso de crise alérgica, procurar atendimento imediatamente.",
                maxLength = MedicalProfileData.MAX_LONG_TEXT_LENGTH
        )
        String observations
) {
    public UpsertMedicalProfileCommand toApplicationData() {
        return new UpsertMedicalProfileCommand(
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

    @Override
    public String toString() {
        return "MedicalProfileRequest[REDACTED]";
    }
}
