package com.jeepclub.backend.health.api.http.dto;

import com.jeepclub.backend.health.core.application.command.UpsertMedicalProfileCommand;
import com.jeepclub.backend.health.core.domain.enums.BloodType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para criação ou atualização de um perfil médico.")
public record MedicalProfileRequest(

        @Schema(
                description = "Tipo sanguíneo do usuário ou dependente.",
                example = "O_POSITIVE"
        )
        BloodType bloodType,

        @Schema(
                description = "Alergias conhecidas.",
                example = "Dipirona, amendoim e picada de abelha."
        )
        @Size(max = 2000, message = "As alergias devem ter no máximo 2000 caracteres.")
        String allergies,

        @Schema(
                description = "Doenças ou condições crônicas relevantes.",
                example = "Asma e hipertensão."
        )
        @Size(max = 2000, message = "As doenças/condições devem ter no máximo 2000 caracteres.")
        String chronicConditions,

        @Schema(
                description = "Medicamentos de uso contínuo.",
                example = "Losartana 50mg diariamente."
        )
        @Size(max = 2000, message = "Os medicamentos devem ter no máximo 2000 caracteres.")
        String continuousMedications,

        @Schema(
                description = "Nome da operadora do convênio médico.",
                example = "Unimed"
        )
        @Size(max = 120, message = "O nome do convênio deve ter no máximo 120 caracteres.")
        String healthInsuranceProvider,

        @Schema(
                description = "Nome ou categoria do plano de saúde.",
                example = "Enfermaria"
        )
        @Size(max = 120, message = "O plano do convênio deve ter no máximo 120 caracteres.")
        String healthInsurancePlan,

        @Schema(
                description = "Número da carteirinha ou identificação do convênio.",
                example = "123456789"
        )
        @Size(max = 80, message = "O número do convênio deve ter no máximo 80 caracteres.")
        String healthInsuranceNumber,

        @Schema(
                description = "Nome do contato de emergência.",
                example = "Maria da Silva"
        )
        @Size(max = 120, message = "O nome do contato de emergência deve ter no máximo 120 caracteres.")
        String emergencyContactName,

        @Schema(
                description = "Telefone do contato de emergência.",
                example = "(12) 99999-9999"
        )
        @Size(max = 20, message = "O telefone do contato de emergência deve ter no máximo 20 caracteres.")
        String emergencyContactPhone,

        @Schema(
                description = "Relação do contato de emergência com o usuário ou dependente.",
                example = "Mãe"
        )
        @Size(max = 80, message = "O parentesco/relação deve ter no máximo 80 caracteres.")
        String emergencyContactRelationship,

        @Schema(
                description = "Observações médicas gerais.",
                example = "Em caso de crise alérgica, procurar atendimento imediatamente."
        )
        @Size(max = 2000, message = "As observações devem ter no máximo 2000 caracteres.")
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
}
