package com.jeepclub.backend.iam.authorization.api.http.controller.admin;

import com.jeepclub.backend.iam.authorization.api.http.dto.permission.PermissionResponseDTO;
import com.jeepclub.backend.iam.authorization.core.application.result.PermissionResult;
import com.jeepclub.backend.iam.authorization.core.application.result.PermissionsResult;
import com.jeepclub.backend.iam.authorization.core.application.service.permission.AdminPermissionService;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
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
                description = "Usuário autenticado não possui AUTHORIZATION_PERMISSION_READ.",
                content = @Content(
                        mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                        schema = @Schema(implementation = ApiErrorResponse.class)
                )
        )
})
public class AdminPermissionController {

    private final AdminPermissionService adminPermissionService;

    @GetMapping
    @Operation(
            summary = "Listar permissões",
            description = "Retorna todas as permissões cadastradas e sincronizadas pelo sistema.",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "Permissões retornadas.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = PermissionResponseDTO.class))
                    )
            )
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
            description = "Retorna os dados de uma permissão a partir do seu identificador.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Permissão retornada.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = PermissionResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Identificador inválido ou permissão inexistente atualmente resulta em erro interno.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
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
            description = "Retorna os dados de uma permissão a partir do seu código técnico.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Permissão retornada.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = PermissionResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Código inválido ou permissão inexistente atualmente resulta em erro interno.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
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
