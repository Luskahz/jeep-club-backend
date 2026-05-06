package com.jeepclub.backend.authorization.api.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

@Schema(description = "Dados necessários para substituir todas as roles vinculadas a um usuário.")
public record ReplaceUserRolesRequestDTO(

        @ArraySchema(
                schema = @Schema(
                        description = "Identificador de uma role.",
                        example = "1"
                ),
                arraySchema = @Schema(
                        description = "Lista de IDs das roles que o usuário deverá possuir. Lista vazia remove todas as roles.",
                        requiredMode = Schema.RequiredMode.REQUIRED
                )
        )
        @NotNull(message = "Lista de roles é obrigatória.")
        List<@Positive(message = "ID da role deve ser positivo.") Long> roleIds
) {
}