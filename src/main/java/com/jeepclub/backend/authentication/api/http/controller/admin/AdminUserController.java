package com.jeepclub.backend.authentication.api.http.controller.admin;

import com.jeepclub.backend.authentication.api.http.dto.admin.user.AdminUserFilterDTO;
import com.jeepclub.backend.authentication.api.http.dto.admin.user.AdminUserResponseDTO;
import com.jeepclub.backend.authentication.core.application.result.admin.user.AdminUserResult;
import com.jeepclub.backend.authentication.core.application.service.user.AdminUserService;
import com.jeepclub.backend.platform.openapi.group.SwaggerOperationGroup;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(
        value = "/authentication/admin/users",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Authentication - Users",
        description = "Operações públicas e administrativas para cadastro, consulta e gerenciamento de usuários."
)
public class AdminUserController {

    private final AdminUserService adminUserService;
    @GetMapping
    @PreAuthorize("hasAuthority('AUTHENTICATION_USER_READ')")
    @RequiredPermission("AUTHENTICATION_USER_READ")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Listar usuários",
            description = """
                Retorna os usuários cadastrados no módulo de autenticação,
                com suporte a paginação, ordenação e filtros.
                """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Usuários retornados com sucesso."
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Parâmetros de consulta inválidos.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Usuário não autenticado.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Usuário sem permissão para consultar usuários.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<Page<AdminUserResponseDTO>> findAll(
            @Valid @ModelAttribute AdminUserFilterDTO filters,
            @PageableDefault(
                    page = 0,
                    size = 20,
                    sort = "id",
                    direction = Sort.Direction.ASC
            )
            Pageable pageable
    ) {
        Page<AdminUserResult> results =
                adminUserService.findAll(filters.toFilter(), pageable);

        return ResponseEntity.ok(
                results.map(AdminUserResponseDTO::from)
        );
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('AUTHENTICATION_USER_READ')")
    @RequiredPermission("AUTHENTICATION_USER_READ")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Consultar usuário",
            description = "Retorna os dados administrativos de um usuário específico.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Usuário retornado com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = AdminUserResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Usuário não encontrado.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<AdminUserResponseDTO> findById(
            @PathVariable @Positive Long userId
    ) {
        AdminUserResult result = adminUserService.findById(userId);

        return ResponseEntity.ok(AdminUserResponseDTO.from(result));
    }

    @PatchMapping("/{userId}/disable")
    @PreAuthorize("hasAuthority('AUTHENTICATION_USER_DISABLE')")
    @RequiredPermission("AUTHENTICATION_USER_DISABLE")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Desativar usuário",
            description = "Desativa administrativamente um usuário cadastrado.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Usuário desativado com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = AdminUserResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Usuário não encontrado.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Usuário já está desativado ou não pode ser desativado no estado atual.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<AdminUserResponseDTO> disable(
            @PathVariable @Positive Long userId
    ) {
        AdminUserResult result = adminUserService.disable(userId);

        return ResponseEntity.ok(AdminUserResponseDTO.from(result));
    }

    @PatchMapping("/{userId}/enable")
    @PreAuthorize("hasAuthority('AUTHENTICATION_USER_ENABLE')")
    @RequiredPermission("AUTHENTICATION_USER_ENABLE")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @Operation(
            summary = "Reativar usuário",
            description = "Reativa administrativamente um usuário desativado.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Usuário reativado com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = AdminUserResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Usuário não encontrado.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Usuário já está ativo ou não pode ser reativado no estado atual.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<AdminUserResponseDTO> enable(
            @PathVariable @Positive Long userId
    ) {
        AdminUserResult result = adminUserService.enable(userId);

        return ResponseEntity.ok(AdminUserResponseDTO.from(result));
    }
}
