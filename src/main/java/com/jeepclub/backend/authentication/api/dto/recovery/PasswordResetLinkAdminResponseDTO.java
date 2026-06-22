package com.jeepclub.backend.authentication.api.dto.recovery;

import com.jeepclub.backend.authentication.core.application.result.PasswordResetLinkAdminResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

@Schema(description = "Resposta da geração de link administrativo de redefinição de senha.")
public record PasswordResetLinkAdminResponseDTO(

        @Schema(
                description = "Link de redefinição de senha que deve ser compartilhado com o usuário.",
                example = "https://jeepclub.com.br/password-recovery/reset?token=dGhpc0lzQVVybFNhZmVUb2tlbl9leGFtcGxl",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String resetLink,

        @Schema(
                description = "Dados da solicitação de recuperação vinculada ao link gerado.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        PasswordRecoveryRequestResponseDTO request
) {

    public static PasswordResetLinkAdminResponseDTO from(
            PasswordResetLinkAdminResult result
    ) {
        Objects.requireNonNull(result, "result cannot be null");

        return new PasswordResetLinkAdminResponseDTO(
                result.resetLink(),
                PasswordRecoveryRequestResponseDTO.from(result.recoveryRequest())
        );
    }
}