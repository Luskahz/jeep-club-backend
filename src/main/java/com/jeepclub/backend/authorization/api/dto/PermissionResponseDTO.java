package com.jeepclub.backend.authorization.api.dto;

import com.jeepclub.backend.authorization.core.domain.enums.ModuleCode;
import com.jeepclub.backend.authorization.core.domain.enums.PermissionCode;
import com.jeepclub.backend.authorization.core.domain.model.Permission;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados de uma permissão")
public record PermissionResponseDTO(

        @Schema(description = "Identificador único da permissão", example = "1")
        Long id,

        @Schema(description = "Código técnico da permissão", example = "AUTHZ_ROLE_CREATE")
        PermissionCode code,

        @Schema(description = "Descrição funcional da permissão", example = "Permite criar papéis de acesso")
        String description,

        @Schema(description = "Módulo ao qual a permissão pertence", example = "AUTHORIZATION")
        ModuleCode module
) {
        public static PermissionResponseDTO from(Permission permission) {
            return new PermissionResponseDTO(
                    permission.getId(),
                    permission.getCode(),
                    permission.getDescription(),
                    permission.getModule()
            );
        }
    }