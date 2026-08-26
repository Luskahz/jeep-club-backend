package com.jeepclub.backend.dependents.api.http.dto.dependent;

import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.br.CPF;

import java.time.LocalDate;

@Schema(description = "Solicitação de criação de um novo dependente.")
public record CreateDependentRequestDTO(

        @Schema(
                description = "Nome completo do dependente.",
                example = "Mariana Silva",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Nome do dependente é obrigatório.")
        String name,

        @Schema(
                description = "CPF do dependente.",
                example = "98765432109",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "CPF do dependente é obrigatório.")
        @CPF(message = "CPF do dependente é inválido.")
        String cpf,

        @Schema(
                description = "Data de nascimento do dependente.",
                example = "2015-08-25",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Data de nascimento do dependente é obrigatória.")
        @PastOrPresent(message = "Data de nascimento não pode estar no futuro.")
        LocalDate birthDate,

        @Schema(
                description = "Tipo de relacionamento com o usuário titular.",
                example = "CHILD",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Tipo de parentesco é obrigatório.")
        RelationshipType relationshipType,

        @Schema(
                description = "Telefone de contato do dependente.",
                example = "11988887777",
                nullable = true
        )
        @Pattern(
                regexp = "^\\d{10,11}$",
                message = "Telefone deve conter 10 ou 11 dígitos."
        )
        String phoneNumber

) {
}
