package com.jeepclub.backend.authentication.api.controller.passwordRecovery;

import com.jeepclub.backend.authentication.api.dto.recovery.*;
import com.jeepclub.backend.authentication.core.application.services.PasswordRecoveryService;
import com.jeepclub.backend.authentication.core.domain.model.PasswordRecoveryRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/authentication/password-recovery/requests")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Authentication - Password Recovery",
        description = "Fluxo público de solicitação e conclusão de recuperação de senha."
)
public class PasswordRecoveryController {

    private final PasswordRecoveryService passwordRecoveryService;

    @PostMapping
    @Operation(
            summary = "Criar ou consultar solicitação de recuperação de senha",
            description = "Cria uma solicitação de recuperação quando não houver uma solicitação ativa para o CPF informado. Caso já exista, retorna os dados da solicitação aberta."
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

    @PostMapping("/token/reset")
    @Operation(
            summary = "Redefinir senha por token",
            description = "Resolve uma solicitação de recuperação usando token recebido por e-mail ou link administrativo."
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

    @PostMapping("/temporary-password/reset")
    @Operation(
            summary = "Redefinir senha usando senha provisória",
            description = "Resolve uma solicitação de recuperação usando CPF e senha provisória gerada por administrador."
    )
    public ResponseEntity<Void> resetPasswordByTemporaryPassword(
            @RequestBody @Valid TemporaryPasswordChangeDTO request
    ) {
        passwordRecoveryService.resetPasswordByTemporaryPassword(
                request.cpf(),
                request.temporaryPassword(),
                request.newPassword()
        );

        return ResponseEntity.noContent().build();
    }
}