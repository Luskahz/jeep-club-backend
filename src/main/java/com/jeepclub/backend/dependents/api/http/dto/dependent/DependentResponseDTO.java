package com.jeepclub.backend.dependents.api.http.dto.dependent;

import com.jeepclub.backend.dependents.core.application.result.DependentResult;
import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
                example = "Mariana Silva",
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
                description = "Tipo de relacionamento com o sócio titular.",
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
                description = "Identificador do sócio titular.",
                example = "5",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long socioId,

        @Schema(
                description = "Status atual do dependente.",
                example = "ACTIVE",
                allowableValues = {"ACTIVE", "DELETED"},
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
                description = "Data da última atualização.",
                example = "2026-05-23T14:20:00Z",
                nullable = true
        )
        Instant updatedAt,

        @Schema(
                description = "Data da exclusão lógica do dependente.",
                example = "2026-08-18T20:30:00Z",
                nullable = true
        )
        Instant deletedAt

) {

    public static DependentResponseDTO from(DependentResult result) {
        Objects.requireNonNull(
                result,
                "Dependent result cannot be null"
        );

        return from(result.dependent());
    }

    public static DependentResponseDTO from(Dependent domain) {
        Objects.requireNonNull(
                domain,
                "Domain model Dependent cannot be null"
        );

        return new DependentResponseDTO(
                domain.getId(),
                domain.getName(),
                domain.getCpf(),
                domain.getBirthDate(),
                domain.getRelationshipType(),
                domain.getPhoneNumber(),
                domain.getSocioId(),
                domain.getStatus(),
                domain.getCreatedAt(),
                domain.getUpdatedAt(),
                domain.getDeletedAt()
        );
    }

    public static List<DependentResponseDTO> from(
            List<Dependent> dependents
    ) {
        Objects.requireNonNull(
                dependents,
                "Dependents list cannot be null"
        );

        return dependents.stream()
                .map(DependentResponseDTO::from)
                .toList();
    }
}