package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.api.dto.recovery.PasswordRecoveryAdminRequestDTO;
import com.jeepclub.backend.authentication.api.dto.recovery.PasswordRecoveryAdminResponseDTO;
import com.jeepclub.backend.authentication.api.dto.recovery.PasswordRecoveryRequestDTO;
import com.jeepclub.backend.authentication.api.dto.recovery.PasswordResetDTO;
import com.jeepclub.backend.authentication.core.application.results.PasswordRecoveryAdminResult;
import com.jeepclub.backend.authentication.core.application.services.PasswordRecoveryService;
import com.jeepclub.backend.infra.config.openapi.security.RequiredPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/recovery")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Authentication - Password Recovery",
        description = "Fluxo de recuperação e redefinição de senha."
)
public class PasswordRecoveryController {

    private final PasswordRecoveryService passwordRecoveryService;

    @PostMapping("/request")
    @Operation(
            summary = "Solicitar recuperação de senha",
            description = "Gera um token seguro e envia um e-mail com link para recuperação de senha, sem revelar se o CPF existe."
    )
    public ResponseEntity<Void> requestRecoveryViaEmail(
            @RequestBody @Valid PasswordRecoveryRequestDTO request
    ) {
        passwordRecoveryService.requestRecoveryViaEmail(request.cpf());

        return ResponseEntity.accepted().build();
    }

    @PostMapping("/reset")
    @Operation(
            summary = "Redefinir senha por token",
            description = "Recebe o token de recuperação e a nova senha para efetivar a troca."
    )
    public ResponseEntity<Void> resetPassword(
            @RequestBody @Valid PasswordResetDTO request
    ) {
        passwordRecoveryService.resetPassword(
                request.token(),
                request.newPassword()
        );

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin-request")
    @PreAuthorize("hasAuthority('AUTHENTICATION_USER_UPDATE')")
    @RequiredPermission("AUTHENTICATION_USER_UPDATE")
    @Operation(
            summary = "Gerar recuperação de senha por administrador",
            description = "Permite que um administrador gere uma senha provisória ou token de recuperação para um usuário."
    )
    public ResponseEntity<PasswordRecoveryAdminResponseDTO> requestRecoveryViaAdmin(
            @RequestBody @Valid PasswordRecoveryAdminRequestDTO request
    ) {
        PasswordRecoveryAdminResult result = passwordRecoveryService.requestRecoveryViaAdmin(
                request.targetUserId(),
                request.generateTempPassword()
        );

        return ResponseEntity.ok(
                new PasswordRecoveryAdminResponseDTO(
                        result.temporaryPassword(),
                        result.resetToken()
                )
        );
    }
}