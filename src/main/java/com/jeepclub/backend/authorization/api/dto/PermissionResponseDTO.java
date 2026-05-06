package com.jeepclub.backend.authorization.api.dto;

import com.jeepclub.backend.authorization.core.domain.model.Permission;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;

@Schema(description = "Dados de uma permissão de autorização.")
public record PermissionResponseDTO(

        @Schema(
                description = "Identificador único da permissão.",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long id,

        @Schema(
                description = "Código técnico único da permissão.",
                example = "AUTHZ_ROLE_CREATE",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String code,

        @Schema(
                description = "Descrição funcional da permissão.",
                example = "Permite criar papéis de acesso.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String description,

        @Schema(
                description = "Módulo ao qual a permissão pertence.",
                example = "AUTHORIZATION",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String module
) {
    public static PermissionResponseDTO from(Permission permission) {
        Objects.requireNonNull(permission, "Permission cannot be null");

        return new PermissionResponseDTO(
                permission.getId(),
                permission.getCode().name(),
                permission.getDescription(),
                permission.getModule().name()
        );
    }

    public static List<PermissionResponseDTO> from(List<Permission> permissions) {
        Objects.requireNonNull(permissions, "Permissions cannot be null");

        return permissions.stream()
                .map(PermissionResponseDTO::from)
                .toList();
    }
}