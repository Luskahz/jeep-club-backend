package com.jeepclub.backend.authentication.api.controller.passwordRecovery;

import com.jeepclub.backend.authentication.api.dto.recovery.PasswordResetLinkAdminResponseDTO;
import com.jeepclub.backend.authentication.api.dto.recovery.TemporaryPasswordAdminResponseDTO;
import com.jeepclub.backend.authentication.core.application.results.PasswordResetLinkAdminResult;
import com.jeepclub.backend.authentication.core.application.results.TemporaryPasswordAdminResult;
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