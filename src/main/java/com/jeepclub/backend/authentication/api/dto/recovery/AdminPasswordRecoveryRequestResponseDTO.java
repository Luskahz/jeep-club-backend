package com.jeepclub.backend.authentication.api.dto.recovery;

import com.jeepclub.backend.authentication.core.application.result.admin.recovery.AdminPasswordRecoveryRequestResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Schema(
        name = "AdminPasswordRecoveryRequestResponse",
        description = "Resposta administrativa com metadados seguros de uma solicitação de recuperação de senha."
)
public record AdminPasswordRecoveryRequestResponseDTO(

        @Schema(
                description = "Identificador único da solicitação de recuperação.",
                example = "100",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long id,

        @Schema(
                description = "Identificador do usuário vinculado à solicitação.",
                example = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Long userId,

        @Schema(
                description = "Status atual da solicitação de recuperação.",
                example = "OPEN",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String status,

        @Schema(
                description = "Origem da solicitação de recuperação.",
                example = "ADMIN_RESET_LINK",
                nullable = true
        )
        String method,

        @Schema(
                description = "Data de criação da solicitação.",
                example = "2026-06-04T17:44:38Z",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Instant createdAt,

        @Schema(
                description = "Data de expiração da solicitação.",
                example = "2026-06-04T18:44:38Z",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Instant expiresAt,

        @Schema(
                description = "Data de resolução da solicitação, quando houver.",
                example = "2026-06-04T18:00:00Z",
                nullable = true
        )
        Instant resolvedAt,

        @Schema(
                description = "Data de cancelamento da solicitação, quando houver.",
                example = "2026-06-04T18:10:00Z",
                nullable = true
        )
        Instant cancelledAt
) {

    public static AdminPasswordRecoveryRequestResponseDTO from(
            AdminPasswordRecoveryRequestResult result
    ) {
        Objects.requireNonNull(result, "result cannot be null");

        return new AdminPasswordRecoveryRequestResponseDTO(
                result.id(),
                result.userId(),
                result.status(),
                result.method(),
                result.createdAt(),
                result.expiresAt(),
                result.resolvedAt(),
                result.cancelledAt()
        );
    }

    public static List<AdminPasswordRecoveryRequestResponseDTO> from(
            List<AdminPasswordRecoveryRequestResult> results
    ) {
        Objects.requireNonNull(results, "results cannot be null");

        return results.stream()
                .map(AdminPasswordRecoveryRequestResponseDTO::from)
                .toList();
    }
}