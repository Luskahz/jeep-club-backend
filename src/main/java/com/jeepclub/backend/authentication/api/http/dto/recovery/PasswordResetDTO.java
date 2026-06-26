package com.jeepclub.backend.authentication.api.http.dto.recovery;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados necessários para redefinir a senha por token de recuperação.")
public record PasswordResetDTO(

        @Schema(
                description = "Token de recuperação enviado ao usuário.",
                example = "eyJhbGciOiJIUzI1NiJ9.recovery-token-example",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Token de recuperação é obrigatório.")
        String token,

        @Schema(
                description = "Nova senha do usuário.",
                example = "NovaSenha@123",
                minLength = 8,
                maxLength = 100,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Nova senha é obrigatória.")
        @Size(min = 8, max = 100, message = "Nova senha deve ter entre 8 e 100 caracteres.")
        String newPassword
) {
}