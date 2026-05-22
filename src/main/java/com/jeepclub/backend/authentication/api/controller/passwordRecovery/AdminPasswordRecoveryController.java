package com.jeepclub.backend.authentication.api.controller.passwordRecovery;

import com.jeepclub.backend.authentication.api.dto.recovery.PasswordResetLinkAdminResponseDTO;
import com.jeepclub.backend.authentication.api.dto.recovery.TemporaryPasswordAdminResponseDTO;
import com.jeepclub.backend.authentication.core.application.results.PasswordResetLinkAdminResult;
import com.jeepclub.backend.authentication.core.application.results.TemporaryPasswordAdminResult;
import com.jeepclub.backend.authentication.core.application.services.PasswordRecoveryService;
import com.jeepclub.backend.infra.config.openapi.security.RequiredPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/authentication/admin/password-recovery/requests")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Authentication - Admin Password Recovery",
        description = "Operações administrativas para suporte à recuperação de senha."
)
public class AdminPasswordRecoveryController {

    private final PasswordRecoveryService passwordRecoveryService;

    @PostMapping("/users/{userId}/temporary-password")
    @PreAuthorize("hasAuthority('AUTHENTICATION_USER_TEMPORARY_PASSWORD_GENERATE')")
    @RequiredPermission("AUTHENTICATION_USER_TEMPORARY_PASSWORD_GENERATE")
    @Operation(
            summary = "Gerar senha provisória",
            description = "Define a solicitação de recuperação como senha provisória, gera uma senha temporária e marca o usuário para troca obrigatória no próximo login."
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
    @PreAuthorize("hasAuthority('AUTHENTICATION_USER_PASSWORD_RESET_LINK_GENERATE')")
    @RequiredPermission("AUTHENTICATION_USER_PASSWORD_RESET_LINK_GENERATE")
    @Operation(
            summary = "Gerar link administrativo de redefinição",
            description = "Define a solicitação de recuperação como link administrativo, gera um token e retorna um link para o administrador compartilhar com o usuário."
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