package com.jeepclub.backend.authorization.api.http.controller.admin;

import com.jeepclub.backend.authorization.api.http.dto.permission.PermissionResponseDTO;
import com.jeepclub.backend.authorization.core.application.result.PermissionResult;
import com.jeepclub.backend.authorization.core.application.result.PermissionsResult;
import com.jeepclub.backend.authorization.core.application.service.permission.AdminPermissionService;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/authorization/permissions")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAuthority('AUTHORIZATION_PERMISSION_READ')")
@RequiredPermission("AUTHORIZATION_PERMISSION_READ")
@Tag(
        name = "Authorization - Permissions",
        description = "Consulta de permissões disponíveis no módulo de autorização."
)
public class AdminPermissionController {

    private final AdminPermissionService adminPermissionService;

    @GetMapping
    @Operation(
            summary = "Listar permissões",
            description = "Retorna todas as permissões cadastradas e sincronizadas pelo sistema."
    )
    public ResponseEntity<List<PermissionResponseDTO>> findAllPermissions() {
        PermissionsResult result = adminPermissionService.findAllPermissions();

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
        PermissionResult result = adminPermissionService.findPermissionById(permissionId);

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
                    example = "AUTHORIZATION_ROLE_CREATE",
                    required = true
            )
            @PathVariable
            @NotBlank(message = "Código da permissão é obrigatório.")
            String permissionCode
    ) {
        PermissionResult result = adminPermissionService.findPermissionByCode(permissionCode);

        return ResponseEntity.ok(
                PermissionResponseDTO.from(result.permission())
        );
    }
}
