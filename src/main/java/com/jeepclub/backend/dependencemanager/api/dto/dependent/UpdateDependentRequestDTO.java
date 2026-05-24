package com.jeepclub.backend.dependencemanager.api.dto.dependent;

import com.jeepclub.backend.dependencemanager.core.domain.enums.RelationshipType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Solicitação de atualização de dados de um dependente.")
public record UpdateDependentRequestDTO(
        @Schema(description = "Nome completo do dependente.", example = "Mariana Silva", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Nome do dependente é obrigatório.")
        String name,

        @Schema(description = "CPF do dependente.", example = "98765432109", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "CPF do dependente é obrigatório.")
        String cpf,

        @Schema(description = "Data de nascimento.", example = "2015-08-25")
        LocalDate birthDate,

        @Schema(description = "Tipo de relacionamento.", example = "CHILD", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Tipo de parentesco é obrigatório.")
        RelationshipType relationshipType,

        @Schema(description = "Telefone de contato.", example = "11988887777")
        String phoneNumber,

        @Schema(description = "Perfil médico do dependente.")
        MedicalProfileDTO medicalProfile,

        @Schema(
                description = "Confirmação explícita de aceite do termo de LGPD para armazenamento de dados do dependente.",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "O aceite de consentimento LGPD é obrigatório para alterar um dependente.")
        Boolean consentAccepted
) {
}

