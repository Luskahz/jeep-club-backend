package com.jeepclub.backend.iam.authorization.api.http.controller.admin;

import com.jeepclub.backend.iam.authorization.api.http.dto.role.RoleResponseDTO;
import com.jeepclub.backend.iam.authorization.api.http.dto.role.CreateRoleRequestDTO;
import com.jeepclub.backend.iam.authorization.api.http.dto.role.UpdateRoleRequestDTO;
import com.jeepclub.backend.iam.authorization.core.application.result.RoleResult;
import com.jeepclub.backend.iam.authorization.core.application.result.RolesResult;
import com.jeepclub.backend.iam.authorization.core.application.service.role.AdminRoleService;
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
import jakarta.validation.Valid;
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
@RequestMapping("/authorization/roles")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Authorization - Roles",
        description = "Gerenciamento de roles de autorização."
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
public class AdminRoleController {

    private final AdminRoleService adminRoleService;

    @PostMapping
    @PreAuthorize("hasAuthority('AUTHORIZATION_ROLE_CREATE')")
    @RequiredPermission("AUTHORIZATION_ROLE_CREATE")
    @Operation(
            summary = "Criar role",
            description = "Cria uma nova role CUSTOM ativa.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Role criada.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = RoleResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Request inválido conforme Bean Validation.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Nome já existente atualmente resulta em erro interno.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<RoleResponseDTO> createRole(
            @RequestBody @Valid CreateRoleRequestDTO request
    ) {
        RoleResult result = adminRoleService.createRole(
                request.name(),
                request.description()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RoleResponseDTO.from(result.role()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('AUTHORIZATION_ROLE_READ')")
    @RequiredPermission("AUTHORIZATION_ROLE_READ")
    @Operation(
            summary = "Listar roles",
            description = "Retorna todas as roles de autorização cadastradas, inclusive inativas ou excluídas logicamente.",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "Roles retornadas.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = RoleResponseDTO.class))
                    )
            )
    )
    public ResponseEntity<List<RoleResponseDTO>> findAllRoles() {
        RolesResult result = adminRoleService.findAllRoles();

        return ResponseEntity.ok(
                RoleResponseDTO.from(result.roles())
        );
    }

    @GetMapping("/{roleId}")
    @PreAuthorize("hasAuthority('AUTHORIZATION_ROLE_READ')")
    @RequiredPermission("AUTHORIZATION_ROLE_READ")
    @Operation(
            summary = "Buscar role por ID",
            description = "Retorna os dados de uma role a partir do seu identificador.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Role retornada.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = RoleResponseDTO.class)
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
    public ResponseEntity<RoleResponseDTO> findRoleById(
            @Parameter(
                    description = "ID da role.",
                    example = "1",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID da role deve ser positivo.")
            Long roleId
    ) {
        RoleResult result = adminRoleService.findRoleById(roleId);

        return ResponseEntity.ok(
                RoleResponseDTO.from(result.role())
        );
    }

    @PutMapping("/{roleId}")
    @PreAuthorize("hasAuthority('AUTHORIZATION_ROLE_UPDATE')")
    @RequiredPermission("AUTHORIZATION_ROLE_UPDATE")
    @Operation(
            summary = "Atualizar role",
            description = "Atualiza nome e descrição de uma role CUSTOM não excluída.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Role atualizada ou retornada sem mudança.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = RoleResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Request inválido conforme Bean Validation.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Identificador, unicidade do nome, estado da role ou ROOT atualmente resultam em erro interno.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<RoleResponseDTO> updateRole(
            @Parameter(
                    description = "ID da role.",
                    example = "1",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID da role deve ser positivo.")
            Long roleId,

            @RequestBody @Valid UpdateRoleRequestDTO request
    ) {
        RoleResult result = adminRoleService.updateRole(
                roleId,
                request.name(),
                request.description()
        );

        return ResponseEntity.ok(
                RoleResponseDTO.from(result.role())
        );
    }

    @PatchMapping("/{roleId}/deactivate")
    @PreAuthorize("hasAuthority('AUTHORIZATION_ROLE_DISABLE')")
    @RequiredPermission("AUTHORIZATION_ROLE_DISABLE")
    @Operation(
            summary = "Desativar role",
            description = "Marca uma role CUSTOM ativa como inativa; repetir a operação retorna a role sem alteração.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Role retornada.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = RoleResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Identificador, role inexistente, excluída ou ROOT atualmente resultam em erro interno.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<RoleResponseDTO> deactivateRole(
            @Parameter(
                    description = "ID da role.",
                    example = "1",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID da role deve ser positivo.")
            Long roleId
    ) {
        RoleResult result = adminRoleService.deactivateRole(roleId);

        return ResponseEntity.ok(
                RoleResponseDTO.from(result.role())
        );
    }

    @PatchMapping("/{roleId}/activate")
    @PreAuthorize("hasAuthority('AUTHORIZATION_ROLE_ENABLE')")
    @RequiredPermission("AUTHORIZATION_ROLE_ENABLE")
    @Operation(
            summary = "Ativar role",
            description = "Marca uma role CUSTOM inativa como ativa; repetir a operação retorna a role sem alteração.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Role retornada.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = RoleResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Identificador, role inexistente, excluída ou ROOT atualmente resultam em erro interno.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<RoleResponseDTO> activateRole(
            @Parameter(
                    description = "ID da role.",
                    example = "1",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID da role deve ser positivo.")
            Long roleId
    ) {
        RoleResult result = adminRoleService.activateRole(roleId);

        return ResponseEntity.ok(
                RoleResponseDTO.from(result.role())
        );
    }

    @DeleteMapping("/{roleId}")
    @PreAuthorize("hasAuthority('AUTHORIZATION_ROLE_DELETE')")
    @RequiredPermission("AUTHORIZATION_ROLE_DELETE")
    @Operation(
            summary = "Excluir role",
            description = "Realiza exclusão lógica de uma role CUSTOM.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Role excluída logicamente."),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Identificador, role inexistente, excluída ou ROOT atualmente resultam em erro interno.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<Void> deleteRole(
            @Parameter(
                    description = "ID da role.",
                    example = "1",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID da role deve ser positivo.")
            Long roleId
    ) {
        adminRoleService.deleteRole(roleId);

        return ResponseEntity.noContent().build();
    }
}
