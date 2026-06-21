package com.jeepclub.backend.authentication.api.controller.passwordRecovery;

import com.jeepclub.backend.authentication.api.dto.recovery.PasswordRecoveryRequestDTO;
import com.jeepclub.backend.authentication.api.dto.recovery.PasswordRecoveryRequestResponseDTO;
import com.jeepclub.backend.authentication.api.dto.recovery.PasswordResetDTO;
import com.jeepclub.backend.authentication.core.application.services.PasswordRecoveryService;
import com.jeepclub.backend.authentication.core.domain.model.PasswordRecoveryRequest;
import com.jeepclub.backend.platform.openapi.group.SwaggerOperationGroup;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
        value = "/authentication/password-recovery/requests",
        produces = MediaType.APPLICATION_JSON_VALUE
)
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Authentication - Password Recovery",
        description = "Fluxos públicos, autenticados e administrativos de recuperação e redefinição de senha."
)
public class PasswordRecoveryRequestController {

    private final PasswordRecoveryService passwordRecoveryService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @SwaggerOperationGroup(value = "Rotas públicas", order = 10)
    @Operation(
            summary = "Criar ou consultar solicitação de recuperação de senha",
            description = """
                    Cria uma solicitação de recuperação de senha para o CPF informado.
                    Caso já exista uma solicitação aberta e válida para o usuário, retorna a solicitação existente.
                    """,
            security = {},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Solicitação de recuperação retornada com sucesso.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = PasswordRecoveryRequestResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Requisição inválida ou dados inconsistentes.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<PasswordRecoveryRequestResponseDTO> createOrGetOpenRecoveryRequest(
            @RequestBody @Valid PasswordRecoveryRequestDTO request
    ) {
        PasswordRecoveryRequest recoveryRequest =
                passwordRecoveryService.createOrGetOpenRecoveryRequest(request.cpf());

        return ResponseEntity.ok(
                PasswordRecoveryRequestResponseDTO.from(recoveryRequest)
        );
    }

    @PostMapping(
            value = "/email-token",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @SwaggerOperationGroup(value = "Rotas públicas", order = 10)
    @Operation(
            summary = "Enviar token de recuperação por e-mail",
            description = """
                    Define o método da solicitação como recuperação por e-mail.
                    Gera um token de redefinição e envia o link para o e-mail cadastrado do usuário.
                    """,
            security = {},
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Token de recuperação gerado e enviado por e-mail.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = PasswordRecoveryRequestResponseDTO.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Requisição inválida ou dados inconsistentes.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<PasswordRecoveryRequestResponseDTO> sendRecoveryEmailToken(
            @RequestBody @Valid PasswordRecoveryRequestDTO request
    ) {
        PasswordRecoveryRequest recoveryRequest =
                passwordRecoveryService.sendRecoveryEmailToken(request.cpf());

        return ResponseEntity.ok(
                PasswordRecoveryRequestResponseDTO.from(recoveryRequest)
        );
    }

    @PostMapping(
            value = "/token/reset",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @SwaggerOperationGroup(value = "Rotas públicas", order = 10)
    @Operation(
            summary = "Redefinir senha por token",
            description = """
                    Redefine a senha do usuário usando um token válido de recuperação.
                    O token pode ter sido gerado por e-mail ou por link administrativo.
                    """,
            security = {},
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Senha redefinida com sucesso.",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Token inválido, expirado ou requisição inconsistente.",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)
                            )
                    )
            }
    )
    public ResponseEntity<Void> resetPasswordByToken(
            @RequestBody @Valid PasswordResetDTO request
    ) {
        passwordRecoveryService.resetPasswordByToken(
                request.token(),
                request.newPassword()
        );

        return ResponseEntity.noContent().build();
    }
}