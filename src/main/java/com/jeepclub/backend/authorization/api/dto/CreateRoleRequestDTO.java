package com.jeepclub.backend.authorization.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

@Schema(description = "Dados necessários para criar uma role de autorização")
public record CreateRoleRequestDTO(

        @Schema(
                description = "Nome da role",
                example = "Administrador de autorização",
                maxLength = 80,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Nome da role é obrigatório.")
        @Size(max = 80, message = "Nome da role deve ter no máximo 80 caracteres.")
        String name,

        @Schema(
                description = "Descrição funcional da role",
                example = "Permite gerenciar roles, permissões e vínculos de usuários",
                maxLength = 255
        )
        @Size(max = 255, message = "Descrição da role deve ter no máximo 255 caracteres.")
        String description,

        @Schema(
                description = "Conjunto de permissões atribuídas à role. Use lista vazia para criar uma role sem permissões.",
                example = "[\"AUTHZ_ROLE_READ\", \"AUTHZ_ROLE_CREATE\", \"AUTHZ_PERMISSION_ASSIGN\"]",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "Lista de permissões é obrigatória. Use [] para criar sem permissões.")
        Set<PermissionCode> permissionCodes
) {
}