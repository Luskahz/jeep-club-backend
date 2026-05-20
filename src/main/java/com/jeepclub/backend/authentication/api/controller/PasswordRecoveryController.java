package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.api.dto.recovery.PasswordRecoveryRequestDTO;
import com.jeepclub.backend.authentication.api.dto.recovery.PasswordResetDTO;
import com.jeepclub.backend.authentication.api.dto.recovery.PasswordResetTokenAdminResponseDTO;
import com.jeepclub.backend.authentication.api.dto.recovery.TemporaryPasswordAdminResponseDTO;
import com.jeepclub.backend.authentication.core.application.results.PasswordResetTokenAdminResult;
import com.jeepclub.backend.authentication.core.application.results.TemporaryPasswordAdminResult;
import com.jeepclub.backend.authentication.core.application.services.PasswordRecoveryService;
import com.jeepclub.backend.infra.config.openapi.security.RequiredPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/authentication/password-recovery")
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

    @PostMapping("/admin/users/{userId}/temporary-password")
    @PreAuthorize("hasAuthority('AUTHENTICATION_USER_TEMPORARY_PASSWORD_GENERATE')")
    @RequiredPermission("AUTHENTICATION_USER_TEMPORARY_PASSWORD_GENERATE")
    @Operation(
            summary = "Gerar senha temporária por administrador",
            description = "Permite que um administrador gere uma senha temporária para um usuário."
    )
    public ResponseEntity<TemporaryPasswordAdminResponseDTO> generateTemporaryPasswordByAdmin(
            @PathVariable @Positive Long userId
    ) {
        TemporaryPasswordAdminResult result = passwordRecoveryService.generateTemporaryPasswordByAdmin(userId);

        return ResponseEntity.ok(
                TemporaryPasswordAdminResponseDTO.from(result)
        );
    }

    @PostMapping("/admin/users/{userId}/reset-token")
    @PreAuthorize("hasAuthority('AUTHENTICATION_USER_PASSWORD_RESET_TOKEN_GENERATE')")
    @RequiredPermission("AUTHENTICATION_USER_PASSWORD_RESET_TOKEN_GENERATE")
    @Operation(
            summary = "Gerar token de redefinição por administrador",
            description = "Permite que um administrador gere um token de redefinição de senha para um usuário."
    )
    public ResponseEntity<PasswordResetTokenAdminResponseDTO> generateResetTokenByAdmin(
            @PathVariable @Positive Long userId
    ) {
        PasswordResetTokenAdminResult result = passwordRecoveryService.generateResetTokenByAdmin(userId);

        return ResponseEntity.ok(
                PasswordResetTokenAdminResponseDTO.from(result)
        );
    }
}