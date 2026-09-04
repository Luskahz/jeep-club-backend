package com.jeepclub.backend.authentication.api.http.dto.session;

import com.jeepclub.backend.authentication.core.application.result.MeResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

@Schema(description = "Dados técnicos da sessão autenticada.")
public record MeResponseDTO(

        @Schema(
                description = "Identificador único do usuário autenticado.",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long userId,

        @Schema(
                description = "Identificador único da sessão autenticada.",
                example = "15",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long sessionId,

        @Schema(
                description = "Indica se a sessão atual ainda está ativa.",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        boolean sessionActive,

        @Schema(
                description = "Tempo restante de validade do access token, em segundos.",
                example = "900",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        long expiresInSeconds
) {
    public static MeResponseDTO from(MeResult result) {
        Objects.requireNonNull(result, "result cannot be null");
        return new MeResponseDTO(
                result.userId(),
                result.sessionId(),
                result.sessionActive(),
                result.expiresInSeconds()
        );
    }
}
