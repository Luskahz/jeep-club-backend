package com.jeepclub.backend.authentication.api.dto.recovery;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para trocar uma senha provisória.")
public record TemporaryPasswordChangeDTO(

        @NotBlank
        @Schema(
                description = "CPF do usuário.",
                example = "12345678900",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String cpf,

        @NotBlank
        @Schema(
                description = "Senha provisória fornecida pelo administrador.",
                example = "Temp@123456",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String temporaryPassword,

        @NotBlank
        @Size(min = 8, max = 72)
        @Schema(
                description = "Nova senha definitiva do usuário.",
                example = "NovaSenha@123",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String newPassword
) {
}