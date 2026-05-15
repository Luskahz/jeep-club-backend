package com.jeepclub.backend.authentication.api.controller;

import com.jeepclub.backend.authentication.api.dto.recovery.PasswordRecoveryAdminRequestDTO;
import com.jeepclub.backend.authentication.api.dto.recovery.PasswordRecoveryAdminResponseDTO;
import com.jeepclub.backend.authentication.api.dto.recovery.PasswordRecoveryRequestDTO;
import com.jeepclub.backend.authentication.api.dto.recovery.PasswordResetDTO;
import com.jeepclub.backend.authentication.core.application.results.PasswordRecoveryAdminResult;
import com.jeepclub.backend.authentication.core.application.services.PasswordRecoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Recuperação de Senha",
        description = "Endpoints para o fluxo de recuperação de senhas (Usuários e Admin)"
)
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth/recovery")
public class PasswordRecoveryController {

    private final PasswordRecoveryService passwordRecoveryService;

    @Operation(
            summary = "Solicitar recuperação de senha via Email",
            description = "Gera um token seguro e envia um e-mail com o link para recuperação de senha."
    )
    @PostMapping("/request")
    public ResponseEntity<Void> requestRecoveryViaEmail(
            @RequestBody @Valid PasswordRecoveryRequestDTO request
    ) {
        passwordRecoveryService.requestRecoveryViaEmail(request.cpf());
        // Sempre retorna 202 Accepted por segurança (não revelar se o CPF existe)
        return ResponseEntity.accepted().build();
    }

    @Operation(
            summary = "Criar nova senha a partir de Token",
            description = "Recebe o token enviado por e-mail e a nova senha para efetivar a troca."
    )
    @PostMapping("/reset")
    public ResponseEntity<Void> resetPassword(
            @RequestBody @Valid PasswordResetDTO request
    ) {
        passwordRecoveryService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Admin: Gerar senha provisória ou Link",
            description = "Exclusivo para administradores: pode gerar uma senha provisória em texto claro ou um link para o usuário."
    )
    @PostMapping("/admin-request")
    public ResponseEntity<PasswordRecoveryAdminResponseDTO> requestRecoveryViaAdmin(
            @RequestBody @Valid PasswordRecoveryAdminRequestDTO request
    ) {
        // Observação: Aqui deveria ter uma validação para garantir que quem chamou é ADMIN.
        // O SecurityFilterChain e o método hasRole("ADMIN") geralmente cuidam disso,
        // mas vale lembrar de checar as authorities se necessário.

        PasswordRecoveryAdminResult result = passwordRecoveryService.requestRecoveryViaAdmin(
                request.targetUserId(),
                request.generateTempPassword()
        );

        PasswordRecoveryAdminResponseDTO response = new PasswordRecoveryAdminResponseDTO(
                result.temporaryPassword(),
                result.resetToken()
        );

        return ResponseEntity.ok(response);
    }
}
