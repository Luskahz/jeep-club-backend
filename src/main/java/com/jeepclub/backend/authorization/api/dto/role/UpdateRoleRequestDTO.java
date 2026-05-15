package com.jeepclub.backend.authorization.api.dto.role;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados necessários para atualizar uma role de autorização.")
public record UpdateRoleRequestDTO(

        @Schema(
                description = "Novo nome único da role de autorização.",
                example = "Administrador de autorização",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Nome da role é obrigatório.")
        @Size(max = 100, message = "Nome da role deve ter no máximo 100 caracteres.")
        String name,

        @Schema(
                description = "Nova descrição funcional da role de autorização.",
                example = "Permite gerenciar roles, permissões e vínculos de usuários.",
                maxLength = 255,
                nullable = true
        )
        @Size(max = 255, message = "Descrição da role deve ter no máximo 255 caracteres.")
        String description
) {
}