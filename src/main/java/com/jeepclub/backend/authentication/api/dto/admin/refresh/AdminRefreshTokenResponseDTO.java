package com.jeepclub.backend.authentication.api.dto.admin.refresh;

import com.jeepclub.backend.authentication.core.application.results.admin.refresh.AdminRefreshTokenResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Schema(
        name = "AdminRefreshTokenResponse",
        description = "Resposta administrativa com metadados seguros de um refresh token."
)
public record AdminRefreshTokenResponseDTO(

        @Schema(
                description = "Identificador único do refresh token.",
                example = "50",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long id,

        @Schema(
                description = "Identificador do usuário vinculado ao refresh token.",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long userId,

        @Schema(
                description = "Identificador da sessão vinculada ao refresh token.",
                example = "10",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long sessionId,

        @Schema(
                description = "Status atual do refresh token.",
                example = "ACTIVE",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String status,

        @Schema(
                description = "Data de criação do refresh token.",
                example = "2026-06-04T17:44:38Z",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Instant createdAt,

        @Schema(
                description = "Data de expiração do refresh token.",
                example = "2026-07-04T17:44:38Z",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Instant expiresAt,

        @Schema(
                description = "Data de revogação do refresh token, quando houver.",
                example = "2026-06-04T18:00:00Z",
                nullable = true
        )
        Instant revokedAt
) {

    public static AdminRefreshTokenResponseDTO from(AdminRefreshTokenResult result) {
        Objects.requireNonNull(result, "result cannot be null");

        return new AdminRefreshTokenResponseDTO(
                result.id(),
                result.userId(),
                result.sessionId(),
                result.status(),
                result.createdAt(),
                result.expiresAt(),
                result.revokedAt()
        );
    }

    public static List<AdminRefreshTokenResponseDTO> from(List<AdminRefreshTokenResult> results) {
        Objects.requireNonNull(results, "results cannot be null");

        return results.stream()
                .map(AdminRefreshTokenResponseDTO::from)
                .toList();
    }
}