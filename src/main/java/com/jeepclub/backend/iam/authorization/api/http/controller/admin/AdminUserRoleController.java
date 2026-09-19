package com.jeepclub.backend.iam.authorization.api.http.controller.admin;

import com.jeepclub.backend.iam.authorization.api.http.dto.role.RoleResponseDTO;
import com.jeepclub.backend.iam.authorization.api.http.dto.userrole.ReplaceUserRolesRequestDTO;
import com.jeepclub.backend.iam.authorization.core.application.result.RolesResult;
import com.jeepclub.backend.iam.authorization.core.application.service.userrole.AdminUserRoleService;
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
@RequestMapping("/authorization/users")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Authorization - User Roles",
        description = "Gerenciamento de roles vinculadas a usuários."
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
public class AdminUserRoleController {

    private final AdminUserRoleService adminUserRoleService;

    @GetMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('AUTHORIZATION_USER_ROLE_READ')")
    @RequiredPermission("AUTHORIZATION_USER_ROLE_READ")
    @Operation(
            summary = "Listar roles de um usuário",
            description = "Retorna todas as roles vinculadas a um usuário, inclusive roles inativas ou excluídas logicamente.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Roles retornadas.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(schema = @Schema(implementation = RoleResponseDTO.class))
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Identificador inválido ou usuário inexistente atualmente resulta em erro interno.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<List<RoleResponseDTO>> findRolesByUser(
            @Parameter(
                    description = "ID do usuário.",
                    example = "1",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID do usuário deve ser positivo.")
            Long userId
    ) {
        RolesResult result = adminUserRoleService.findRolesByUserId(userId);

        return ResponseEntity.ok(
                RoleResponseDTO.from(result.roles())
        );
    }

    @PutMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('AUTHORIZATION_USER_ROLE_ASSIGN') and hasAuthority('AUTHORIZATION_USER_ROLE_REVOKE')")
    @RequiredPermission({
            "AUTHORIZATION_USER_ROLE_ASSIGN",
            "AUTHORIZATION_USER_ROLE_REVOKE"
    })
    @Operation(
            summary = "Substituir roles de um usuário",
            description = "Substitui os vínculos CUSTOM do usuário. Uma lista vazia remove todas as roles CUSTOM; vínculos ROOT existentes são preservados.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Roles CUSTOM substituídas."),
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
                            description = "Identificador, usuário ou role inexistente, ROOT solicitada ou role inativa atualmente resultam em erro interno.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<Void> replaceUserRoles(
            @Parameter(
                    description = "ID do usuário.",
                    example = "1",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID do usuário deve ser positivo.")
            Long userId,

            @RequestBody @Valid ReplaceUserRolesRequestDTO request
    ) {
        adminUserRoleService.replaceUserRoles(
                userId,
                request.roleIds()
        );

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("hasAuthority('AUTHORIZATION_USER_ROLE_ASSIGN')")
    @RequiredPermission("AUTHORIZATION_USER_ROLE_ASSIGN")
    @Operation(
            summary = "Atribuir role a um usuário",
            description = "Cria o vínculo entre um usuário e uma role CUSTOM ativa.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Vínculo criado."),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Identificador, usuário ou role inexistente, ROOT, role inativa ou vínculo duplicado atualmente resultam em erro interno.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<Void> assignRoleToUser(
            @Parameter(
                    description = "ID do usuário.",
                    example = "1",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID do usuário deve ser positivo.")
            Long userId,

            @Parameter(
                    description = "ID da role.",
                    example = "10",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID da role deve ser positivo.")
            Long roleId
    ) {
        adminUserRoleService.assignRoleToUser(userId, roleId);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("hasAuthority('AUTHORIZATION_USER_ROLE_REVOKE')")
    @RequiredPermission("AUTHORIZATION_USER_ROLE_REVOKE")
    @Operation(
            summary = "Remover role de um usuário",
            description = "Remove o vínculo entre um usuário e uma role CUSTOM.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Vínculo removido."),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Identificador, usuário ou role inexistente, ROOT ou vínculo ausente atualmente resultam em erro interno.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<Void> revokeRoleFromUser(
            @Parameter(
                    description = "ID do usuário.",
                    example = "1",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID do usuário deve ser positivo.")
            Long userId,

            @Parameter(
                    description = "ID da role.",
                    example = "10",
                    required = true
            )
            @PathVariable
            @Positive(message = "ID da role deve ser positivo.")
            Long roleId
    ) {
        adminUserRoleService.revokeRoleFromUser(userId, roleId);

        return ResponseEntity.noContent().build();
    }
}
