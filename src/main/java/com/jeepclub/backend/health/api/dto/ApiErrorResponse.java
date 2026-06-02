package com.jeepclub.backend.health.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Resposta padronizada de erro da API.")
public record ApiErrorResponse(
        @Schema(description = "Data e hora do erro.")
        Instant timestamp,

        @Schema(description = "Código HTTP do erro.", example = "400")
        int status,

        @Schema(description = "Descrição resumida do erro.", example = "Bad Request")
        String error,

        @Schema(description = "Mensagem principal do erro.", example = "O telefone de emergência deve ter 10 ou 11 dígitos.")
        String message,

        @Schema(description = "Lista de detalhes adicionais do erro.")
        List<String> details
) {
    public static ApiErrorResponse of(
            int status,
            String error,
            String message
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status,
                error,
                message,
                List.of()
        );
    }

    public static ApiErrorResponse of(
            int status,
            String error,
            String message,
            List<String> details
    ) {
        return new ApiErrorResponse(
                Instant.now(),
                status,
                error,
                message,
                details
        );
    }
}
