package com.jeepclub.backend.authorization.api.dto.role;


import com.jeepclub.backend.authorization.api.dto.PermissionResponseDTO;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "Detalhes completos de uma role")
public record RoleDetailsResponseDTO(

        @Schema(description = "Identificador único da role", example = "1")
        Long id,

        @Schema(description = "Nome da role", example = "Administrador de autorização")
        String name,

        @Schema(description = "Descrição funcional da role", example = "Permite gerenciar roles, permissões e vínculos de usuários")
        String description,

        @Schema(description = "Status atual da role", example = "ACTIVE")
        RoleStatus status,

        @Schema(description = "Permissões vinculadas à role")
        Set<PermissionResponseDTO> permissions
) {
}