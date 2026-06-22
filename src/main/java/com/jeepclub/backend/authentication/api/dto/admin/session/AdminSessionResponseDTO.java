package com.jeepclub.backend.authentication.api.dto.admin.session;

import com.jeepclub.backend.authentication.core.application.result.admin.session.AdminSessionResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Schema(
        name = "AdminSessionResponse",
        description = "Resposta administrativa com metadados seguros de uma sessão."
)
public record AdminSessionResponseDTO(

        @Schema(
                description = "Identificador único da sessão.",
                example = "10",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long id,

        @Schema(
                description = "Identificador do usuário vinculado à sessão.",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long userId,

        @Schema(
                description = "Status atual da sessão.",
                example = "ACTIVE",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String status,

        @Schema(
                description = "Data de criação da sessão.",
                example = "2026-06-04T17:44:38Z",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Instant createdAt,

        @Schema(
                description = "Data de expiração da sessão.",
                example = "2026-06-05T17:44:38Z",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Instant expiresAt,

        @Schema(
                description = "Data de encerramento da sessão, quando houver.",
                example = "2026-06-04T18:00:00Z",
                nullable = true
        )
        Instant loggedOutAt
) {

    public static AdminSessionResponseDTO from(AdminSessionResult result) {
        Objects.requireNonNull(result, "result cannot be null");

        return new AdminSessionResponseDTO(
                result.id(),
                result.userId(),
                result.status(),
                result.createdAt(),
                result.expiresAt(),
                result.loggedOutAt()
        );
    }

    public static List<AdminSessionResponseDTO> from(List<AdminSessionResult> results) {
        Objects.requireNonNull(results, "results cannot be null");

        return results.stream()
                .map(AdminSessionResponseDTO::from)
                .toList();
    }
}