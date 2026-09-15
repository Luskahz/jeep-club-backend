package com.jeepclub.backend.iam.authorization.api.http.controller.admin;

import com.jeepclub.backend.iam.authorization.api.http.dto.permission.PermissionResponseDTO;
import com.jeepclub.backend.iam.authorization.core.application.result.PermissionsResult;
import com.jeepclub.backend.iam.authorization.core.application.service.rolepermission.AdminRolePermissionService;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@ApiResponses({
        @ApiResponse(
                responseCode = "401",
                description = "Usuário não autenticado.",
                content = @Content(
                        mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ApiErrorResponse.class)
                )
        ),
        @ApiResponse(
                responseCode = "403",
                description = "Usuário autenticado não possui a permission exigida pela operação.",
                content = @Content(
                        mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ApiErrorResponse.class)
                )
        )
})
public class AdminRolePermissionController {

    private final AdminRolePermissionService adminRolePermissionService;

    @GetMapping
    @PreAuthorize("hasAuthority('AUTHORIZATION_PERMISSION_READ')")
    @RequiredPermission("AUTHORIZATION_PERMISSION_READ")
    @Operation(
            summary = "Listar permissões de uma role",
            description = "Retorna todas as permissões vinculadas a uma role de autorização.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Permissões retornadas.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(schema = @Schema(implementation = PermissionResponseDTO.class))
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Identificador inválido ou role inexistente atualmente resulta em erro interno.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
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
        PermissionsResult result = adminRolePermissionService.findPermissionsByRoleId(roleId);

        return ResponseEntity.ok(
                PermissionResponseDTO.from(result.permissions())
        );
    }

    @PostMapping("/{permissionId}")
    @PreAuthorize("hasAuthority('AUTHORIZATION_PERMISSION_ASSIGN')")
    @RequiredPermission("AUTHORIZATION_PERMISSION_ASSIGN")
    @Operation(
            summary = "Atribuir permissão a uma role",
            description = "Cria o vínculo entre uma role CUSTOM ativa e uma permissão.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Vínculo criado."),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Identificador, role ou permissão inexistente, ROOT, role inativa ou vínculo duplicado atualmente resultam em erro interno.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
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
        adminRolePermissionService.assignPermissionToRole(roleId, permissionId);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{permissionId}")
    @PreAuthorize("hasAuthority('AUTHORIZATION_PERMISSION_REVOKE')")
    @RequiredPermission("AUTHORIZATION_PERMISSION_REVOKE")
    @Operation(
            summary = "Remover permissão de uma role",
            description = "Remove o vínculo entre uma role CUSTOM e uma permissão.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Vínculo removido."),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Identificador, role ou permissão inexistente, ROOT ou vínculo ausente atualmente resultam em erro interno.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
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
        adminRolePermissionService.removePermissionFromRole(roleId, permissionId);

        return ResponseEntity.noContent().build();
    }
}
