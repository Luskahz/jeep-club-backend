package com.jeepclub.backend.authorization.api.dto;

import com.jeepclub.backend.authorization.core.domain.enums.RoleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo de uma role")
public record RoleSimpleResponseDTO(

        @Schema(description = "Identificador único da role", example = "1")
        Long id,

        @Schema(description = "Nome da role", example = "Administrador de autorização")
        String name,

        @Schema(description = "Status atual da role", example = "ACTIVE")
        RoleStatus status
) {
}