package com.jeepclub.backend.authorization.api.controller;

import com.jeepclub.backend.authorization.api.dto.PermissionResponseDTO;
import com.jeepclub.backend.authorization.core.application.result.PermissionsResult;
import com.jeepclub.backend.authorization.core.application.service.RolePermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/authorization/roles/{roleId}/permissions")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Authorization - Role Permissions",
        description = "Gerenciamento de permissões vinculadas a roles."
)
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    @GetMapping
    @Operation(
            summary = "Listar permissões de uma role",
            description = "Retorna todas as permissões vinculadas a uma role de autorização."
    )
    public ResponseEntity<List<PermissionResponseDTO>> findPermissionsByRoleId(
            @Parameter(
                    description = "ID da role.",
                    example = "1",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID da role deve ser positivo.")
            Long roleId
    ) {
        PermissionsResult result = rolePermissionService.findPermissionsByRoleId(roleId);

        return ResponseEntity.ok(
                PermissionResponseDTO.from(result.permissions())
        );
    }

    @PostMapping("/{permissionId}")
    @Operation(
            summary = "Atribuir permissão a uma role",
            description = "Cria o vínculo entre uma role e uma permissão."
    )
    public ResponseEntity<Void> assignPermissionToRole(
            @Parameter(
                    description = "ID da role.",
                    example = "1",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID da role deve ser positivo.")
            Long roleId,

            @Parameter(
                    description = "ID da permissão.",
                    example = "10",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID da permissão deve ser positivo.")
            Long permissionId
    ) {
        rolePermissionService.assignPermissionToRole(roleId, permissionId);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{permissionId}")
    @Operation(
            summary = "Remover permissão de uma role",
            description = "Remove o vínculo entre uma role e uma permissão."
    )
    public ResponseEntity<Void> removePermissionFromRole(
            @Parameter(
                    description = "ID da role.",
                    example = "1",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID da role deve ser positivo.")
            Long roleId,

            @Parameter(
                    description = "ID da permissão.",
                    example = "10",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID da permissão deve ser positivo.")
            Long permissionId
    ) {
        rolePermissionService.removePermissionFromRole(roleId, permissionId);

        return ResponseEntity.noContent().build();
    }
}