package com.jeepclub.backend.authorization.api.dto;

import com.jeepclub.backend.authorization.core.domain.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Dados de uma role de autorização")
public record RoleResponseDTO(

        @Schema(description = "Identificador único da role", example = "1")
        Long id,

        @Schema(description = "Nome da role", example = "Administrador de autorização")
        String name,

        @Schema(description = "Descrição funcional da role", example = "Permite gerenciar roles, permissões e vínculos de usuários")
        String description,

        @Schema(description = "Status da role", example = "ACTIVE")
        String status,

        @Schema(description = "Data de criação da role")
        Instant createdAt,

        @Schema(description = "Data da última atualização da role")
        Instant updatedAt,

        @Schema(description = "Data de exclusão lógica da role")
        Instant deletedAt
) {
    public static RoleResponseDTO from(Role role) {
        return new RoleResponseDTO(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.getStatus().name(),
                role.getCreatedAt(),
                role.getUpdatedAt(),
                role.getDeletedAt()
        );
    }

    public static List<RoleResponseDTO> from(List<Role> roles) {
        return roles.stream()
                .map(RoleResponseDTO::from)
                .toList();
    }
}