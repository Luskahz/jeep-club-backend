package com.jeepclub.backend.authorization.api.dto;

import com.jeepclub.backend.authorization.core.domain.model.Permission;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Dados de uma permissão")
public record PermissionResponseDTO(

        @Schema(description = "Identificador único da permissão", example = "1")
        Long id,

        @Schema(description = "Código técnico da permissão", example = "AUTHZ_ROLE_CREATE")
        String code,

        @Schema(description = "Descrição funcional da permissão", example = "Permite criar papéis de acesso")
        String description,

        @Schema(description = "Módulo ao qual a permissão pertence", example = "AUTHORIZATION")
        String module
) {
    public static PermissionResponseDTO from(Permission permission) {
        return new PermissionResponseDTO(
                permission.getId(),
                permission.getCode().name(),
                permission.getDescription(),
                permission.getModule().name()
        );
    }

    public static List<PermissionResponseDTO> from(List<Permission> permissions) {
        return permissions.stream()
                .map(PermissionResponseDTO::from)
                .toList();
    }
}