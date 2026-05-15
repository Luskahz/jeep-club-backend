package com.jeepclub.backend.authorization.api.dto;

import com.jeepclub.backend.authorization.core.domain.model.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Schema(description = "Dados de uma role de autorização.")
public record RoleResponseDTO(

        @Schema(
                description = "Identificador único da role.",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long id,

        @Schema(
                description = "Nome da role de autorização.",
                example = "Administrador de autorização",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String name,

        @Schema(
                description = "Descrição funcional da role de autorização.",
                example = "Permite gerenciar roles, permissões e vínculos de usuários.",
                nullable = true
        )
        String description,

        @Schema(
                description = "Status atual da role.",
                example = "ACTIVE",
                allowableValues = {"ACTIVE", "INACTIVE", "DELETED"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String status,

        @Schema(
                description = "Data de criação da role.",
                example = "2026-05-06T17:30:00Z",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Instant createdAt,

        @Schema(
                description = "Data da última atualização da role.",
                example = "2026-05-06T18:10:00Z",
                nullable = true
        )
        Instant updatedAt,

        @Schema(
                description = "Data de exclusão lógica da role.",
                example = "2026-05-06T19:00:00Z",
                nullable = true
        )
        Instant deletedAt
) {
    public static RoleResponseDTO from(Role role) {
        Objects.requireNonNull(role, "Role cannot be null");

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
        Objects.requireNonNull(roles, "Roles cannot be null");

        return roles.stream()
                .map(RoleResponseDTO::from)
                .toList();
    }
}