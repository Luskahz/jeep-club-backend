package com.jeepclub.backend.authentication.api.controller.passwordRecovery;

import com.jeepclub.backend.authentication.api.dto.recovery.AdminPasswordRecoveryRequestResponseDTO;
import com.jeepclub.backend.authentication.api.dto.recovery.PasswordResetLinkAdminResponseDTO;
import com.jeepclub.backend.authentication.api.dto.recovery.TemporaryPasswordAdminResponseDTO;
import com.jeepclub.backend.authentication.core.application.results.PasswordResetLinkAdminResult;
import com.jeepclub.backend.authentication.core.application.results.TemporaryPasswordAdminResult;
import com.jeepclub.backend.authentication.core.application.results.admin.recovery.AdminPasswordRecoveryRequestResult;
import com.jeepclub.backend.authentication.core.application.services.PasswordRecoveryService;
import com.jeepclub.backend.platform.openapi.group.SwaggerOperationGroup;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(
        value = "/authentication/admin/password-recovery/requests",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Authentication - Password Recovery",
        description = "Fluxos públicos, autenticados e administrativos de recuperação e redefinição de senha."
)
public class AdminPasswordRecoveryRequestController {

    private final PasswordRecoveryService passwordRecoveryService;

    @GetMapping
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @PreAuthorize("hasAuthority('AUTHENTICATION_PASSWORD_RECOVERY_READ')")
    @RequiredPermission("AUTHENTICATION_PASSWORD_RECOVERY_READ")
    @Operation(
            summary = "Listar solicitações de recuperação",
            description = """
                    Lista solicitações administrativas de recuperação de senha.
                    Não retorna tokens brutos nem hashes de recuperação.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Solicitações retornadas com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = AdminPasswordRecoveryRequestResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Usuário autenticado não possui permissão para consultar solicitações de recuperação.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<List<AdminPasswordRecoveryRequestResponseDTO>> findAll() {
        List<AdminPasswordRecoveryRequestResult> results =
                passwordRecoveryService.findAllRecoveryRequestsByAdmin();

        return ResponseEntity.ok(
                AdminPasswordRecoveryRequestResponseDTO.from(results)
        );
    }

    @GetMapping("/{requestId}")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @PreAuthorize("hasAuthority('AUTHENTICATION_PASSWORD_RECOVERY_READ')")
    @RequiredPermission("AUTHENTICATION_PASSWORD_RECOVERY_READ")
    @Operation(
            summary = "Consultar solicitação de recuperação",
            description = """
                    Consulta uma solicitação administrativa de recuperação de senha.
                    Não retorna token bruto nem hash de recuperação.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Solicitação retornada com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = AdminPasswordRecoveryRequestResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Usuário autenticado não possui permissão para consultar solicitações de recuperação.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Solicitação de recuperação não encontrada.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<AdminPasswordRecoveryRequestResponseDTO> findById(
            @PathVariable @Positive Long requestId
    ) {
        AdminPasswordRecoveryRequestResult result =
                passwordRecoveryService.findRecoveryRequestByIdByAdmin(requestId);

        return ResponseEntity.ok(
                AdminPasswordRecoveryRequestResponseDTO.from(result)
        );
    }

    @GetMapping("/users/{userId}")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @PreAuthorize("hasAuthority('AUTHENTICATION_PASSWORD_RECOVERY_READ')")
    @RequiredPermission("AUTHENTICATION_PASSWORD_RECOVERY_READ")
    @Operation(
            summary = "Listar solicitações de recuperação de um usuário",
            description = """
                    Lista solicitações de recuperação de senha vinculadas ao usuário informado.
                    Não retorna tokens brutos nem hashes de recuperação.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Solicitações do usuário retornadas com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = AdminPasswordRecoveryRequestResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Usuário autenticado não possui permissão para consultar solicitações de recuperação.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Usuário alvo não encontrado.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<List<AdminPasswordRecoveryRequestResponseDTO>> findByUserId(
            @PathVariable @Positive Long userId
    ) {
        List<AdminPasswordRecoveryRequestResult> results =
                passwordRecoveryService.findRecoveryRequestsByUserIdByAdmin(userId);

        return ResponseEntity.ok(
                AdminPasswordRecoveryRequestResponseDTO.from(results)
        );
    }

    @PatchMapping("/{requestId}/cancel")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @PreAuthorize("hasAuthority('AUTHENTICATION_PASSWORD_RECOVERY_CANCEL')")
    @RequiredPermission("AUTHENTICATION_PASSWORD_RECOVERY_CANCEL")
    @Operation(
            summary = "Cancelar solicitação de recuperação",
            description = """
                    Cancela administrativamente uma solicitação de recuperação de senha.
                    Uma solicitação já resolvida, expirada ou cancelada não deve ser cancelada novamente.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Solicitação cancelada com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = AdminPasswordRecoveryRequestResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Usuário autenticado não possui permissão para cancelar solicitações de recuperação.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Solicitação de recuperação não encontrada.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "Solicitação já está cancelada, resolvida, expirada ou não pode ser cancelada no estado atual.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<AdminPasswordRecoveryRequestResponseDTO> cancel(
            @PathVariable @Positive Long requestId
    ) {
        AdminPasswordRecoveryRequestResult result =
                passwordRecoveryService.cancelRecoveryRequestByAdmin(requestId);

        return ResponseEntity.ok(
                AdminPasswordRecoveryRequestResponseDTO.from(result)
        );
    }

    @PostMapping("/users/{userId}/temporary-password")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @PreAuthorize("hasAuthority('AUTHENTICATION_USER_TEMPORARY_PASSWORD_GENERATE')")
    @RequiredPermission("AUTHENTICATION_USER_TEMPORARY_PASSWORD_GENERATE")
    @Operation(
            summary = "Gerar senha provisória",
            description = """
                    Gera uma senha provisória para o usuário informado.
                    Marca o usuário para troca obrigatória de senha no próximo login.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Senha provisória gerada com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = TemporaryPasswordAdminResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Identificador de usuário inválido ou operação inconsistente.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Usuário autenticado não possui permissão para gerar senha provisória.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Usuário alvo não encontrado.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<TemporaryPasswordAdminResponseDTO> generateTemporaryPassword(
            @PathVariable @Positive Long userId
    ) {
        TemporaryPasswordAdminResult result =
                passwordRecoveryService.generateTemporaryPasswordByAdmin(userId);

        return ResponseEntity.ok(
                TemporaryPasswordAdminResponseDTO.from(result)
        );
    }

    @PostMapping("/users/{userId}/reset-link")
    @SwaggerOperationGroup(value = "Rotas administrativas", order = 30)
    @PreAuthorize("hasAuthority('AUTHENTICATION_USER_PASSWORD_RESET_LINK_GENERATE')")
    @RequiredPermission("AUTHENTICATION_USER_PASSWORD_RESET_LINK_GENERATE")
    @Operation(
            summary = "Gerar link administrativo de redefinição",
            description = """
                    Gera um link administrativo de redefinição de senha para o usuário informado.
                    O administrador deve compartilhar o link com o usuário final.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Link administrativo de redefinição gerado com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = PasswordResetLinkAdminResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Identificador de usuário inválido ou operação inconsistente.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "403",
                            description = "Usuário autenticado não possui permissão para gerar link administrativo.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Usuário alvo não encontrado.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<PasswordResetLinkAdminResponseDTO> generateResetLink(
            @PathVariable @Positive Long userId
    ) {
        PasswordResetLinkAdminResult result =
                passwordRecoveryService.generateResetLinkByAdmin(userId);

        return ResponseEntity.ok(
                PasswordResetLinkAdminResponseDTO.from(result)
        );
    }
}