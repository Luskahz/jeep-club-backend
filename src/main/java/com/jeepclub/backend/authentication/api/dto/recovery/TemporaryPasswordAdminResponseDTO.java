package com.jeepclub.backend.authentication.api.dto.recovery;

import com.jeepclub.backend.authentication.core.application.results.TemporaryPasswordAdminResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

@Schema(description = "Resposta da geração de senha temporária por administrador.")
public record TemporaryPasswordAdminResponseDTO(

        @Schema(
                description = "Senha temporária gerada para o usuário.",
                example = "A9x@72Lm#pQ1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String temporaryPassword
) {
    public static TemporaryPasswordAdminResponseDTO from(TemporaryPasswordAdminResult result) {
        Objects.requireNonNull(result, "result cannot be null");

        return new TemporaryPasswordAdminResponseDTO(
                result.temporaryPassword()
        );
    }
}