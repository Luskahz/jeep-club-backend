package com.jeepclub.backend.authentication.api.dto.recovery;

import com.jeepclub.backend.authentication.core.application.results.PasswordResetTokenAdminResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

@Schema(description = "Resposta da geração de token de redefinição por administrador.")
public record PasswordResetTokenAdminResponseDTO(

        @Schema(
                description = "Token bruto de redefinição de senha. Deve ser entregue ao usuário por canal seguro.",
                example = "dGhpc0lzQVVybFNhZmVUb2tlbl9leGFtcGxl",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String resetToken
) {
    public static PasswordResetTokenAdminResponseDTO from(PasswordResetTokenAdminResult result) {
        Objects.requireNonNull(result, "result cannot be null");

        return new PasswordResetTokenAdminResponseDTO(
                result.resetToken()
        );
    }
}