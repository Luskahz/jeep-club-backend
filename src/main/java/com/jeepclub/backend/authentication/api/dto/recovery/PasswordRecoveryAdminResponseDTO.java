package com.jeepclub.backend.authentication.api.dto.recovery;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados gerados para recuperação de senha por administrador.")
public record PasswordRecoveryAdminResponseDTO(

        @Schema(
                description = "Senha provisória gerada em texto claro. Retorna nulo quando a recuperação for feita por token.",
                example = "Temp@12345",
                nullable = true
        )
        String temporaryPassword,

        @Schema(
                description = "Token de recuperação de senha. Retorna nulo quando a recuperação for feita por senha provisória.",
                example = "eyJhbGciOiJIUzI1NiJ9.recovery-token-example",
                nullable = true
        )
        String resetToken
) {
}