package com.jeepclub.backend.authorization.api.controller;

import com.jeepclub.backend.authorization.api.dto.PermissionResponseDTO;
import com.jeepclub.backend.authorization.core.application.result.PermissionResult;
import com.jeepclub.backend.authorization.core.application.result.PermissionsResult;
import com.jeepclub.backend.authorization.core.application.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/authorization/permissions")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Authorization - Permissions",
        description = "Consulta de permissões disponíveis no módulo de autorização."
)
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    @Operation(
            summary = "Listar permissões",
            description = "Retorna todas as permissões cadastradas e sincronizadas pelo sistema."
    )
    public ResponseEntity<List<PermissionResponseDTO>> findAllPermissions() {
        PermissionsResult result = permissionService.findAllPermissions();

        return ResponseEntity.ok(
                PermissionResponseDTO.from(result.permissions())
        );
    }

    @GetMapping("/{permissionId}")
    @Operation(
            summary = "Buscar permissão por ID",
            description = "Retorna os dados de uma permissão a partir do seu identificador."
    )
    public ResponseEntity<PermissionResponseDTO> findPermissionById(
            @Parameter(
                    description = "ID da permissão.",
                    example = "1",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID da permissão deve ser positivo.")
            Long permissionId
    ) {
        PermissionResult result = permissionService.findPermissionById(permissionId);

        return ResponseEntity.ok(
                PermissionResponseDTO.from(result.permission())
        );
    }

    @GetMapping("/code/{permissionCode}")
    @Operation(
            summary = "Buscar permissão por código",
            description = "Retorna os dados de uma permissão a partir do seu código técnico."
    )
    public ResponseEntity<PermissionResponseDTO> findPermissionByCode(
            @Parameter(
                    description = "Código técnico da permissão.",
                    example = "AUTHZ_ROLE_CREATE",
                    required = true
            )
            @PathVariable
            String permissionCode
    ) {
        PermissionResult result = permissionService.findPermissionByCode(permissionCode);

        return ResponseEntity.ok(
                PermissionResponseDTO.from(result.permission())
        );
    }
}