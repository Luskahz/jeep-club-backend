package com.jeepclub.backend.dependents.api.http.dto.dependent;

import com.jeepclub.backend.dependents.core.application.result.DependentResult;
import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Schema(description = "Dados de um dependente cadastrado no Jeep Club.")
public record DependentResponseDTO(

        @Schema(
                description = "Identificador único do dependente.",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long id,

        @Schema(
                description = "Nome completo do dependente.",
                example = "Maria Silva",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String name,

        @Schema(
                description = "CPF do dependente.",
                example = "98765432109",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String cpf,

        @Schema(
                description = "Data de nascimento do dependente.",
                example = "2015-08-25",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        LocalDate birthDate,

        @Schema(
                description = "Tipo de relacionamento com o usuário titular.",
                example = "CHILD",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        RelationshipType relationshipType,

        @Schema(
                description = "Telefone de contato do dependente.",
                example = "11988887777",
                nullable = true
        )
        String phoneNumber,

        @Schema(
                description = "Identificador do usuário titular.",
                example = "5",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long userId,

        @Schema(
                description = "Status atual do dependente.",
                example = "ACTIVE",
                allowableValues = {"ACTIVE", "DISABLED"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        DependentStatus status,

        @Schema(
                description = "Data de criação do dependente.",
                example = "2026-05-22T21:30:00Z",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Instant createdAt,

        @Schema(
                description = "Data da última atualização do dependente.",
                example = "2026-05-23T14:20:00Z",
                nullable = true
        )
        Instant updatedAt

) {

    public static DependentResponseDTO from(DependentResult result) {
        Objects.requireNonNull(
                result,
                "Dependent result cannot be null"
        );

        return new DependentResponseDTO(
                result.id(),
                result.name(),
                result.cpf(),
                result.birthDate(),
                result.relationshipType(),
                result.phoneNumber(),
                result.userId(),
                result.status(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}
