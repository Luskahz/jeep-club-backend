package com.jeepclub.backend.iam.authentication.api.http.dto.session;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados necessários para concluir a troca obrigatória de senha.")
public record CompleteRequiredPasswordChangeDTO(

        @Schema(
                description = "Token temporário retornado pelo login com troca de senha obrigatória.",
                example = "u9Tt8sGv2xYpQz...",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        String passwordChangeToken,

        @Schema(
                description = "Nova senha definitiva do usuário.",
                example = "NovaSenha@123",
                minLength = 8,
                maxLength = 72,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank
        @Size(min = 8, max = 72)
        String newPassword
) {
}
