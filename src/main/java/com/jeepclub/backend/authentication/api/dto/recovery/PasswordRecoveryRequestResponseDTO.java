package com.jeepclub.backend.authentication.api.dto.recovery;

import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestStatus;
import com.jeepclub.backend.authentication.core.domain.model.PasswordRecoveryRequest;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Objects;

@Schema(description = "Dados da solicitação de recuperação de senha.")
public record PasswordRecoveryRequestResponseDTO(

        @Schema(
                description = "Status atual da solicitação.",
                example = "OPEN",
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"OPEN", "RESOLVED", "CANCELLED"}
        )
        PasswordRecoveryRequestStatus status,

        @Schema(
                description = "Método usado para resolver a recuperação.",
                example = "UNDEFINED",
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"UNDEFINED", "EMAIL_TOKEN", "ADMIN_RESET_LINK", "ADMIN_TEMPORARY_PASSWORD"}
        )
        PasswordRecoveryRequestMethod method,

        @Schema(
                description = "Data de criação da solicitação.",
                example = "2026-05-20T18:30:00Z",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Instant createdAt,

        @Schema(
                description = "Data de expiração da solicitação.",
                example = "2026-05-20T19:30:00Z",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        Instant expiresAt,

        @Schema(
                description = "Data de resolução da solicitação, quando houver.",
                example = "2026-05-20T18:45:00Z",
                nullable = true
        )
        Instant resolvedAt,

        @Schema(
                description = "Data de cancelamento da solicitação, quando houver.",
                example = "2026-05-20T18:40:00Z",
                nullable = true
        )
        Instant cancelledAt
) {

    public static PasswordRecoveryRequestResponseDTO from(
            PasswordRecoveryRequest request
    ) {
        Objects.requireNonNull(request, "request cannot be null");

        return new PasswordRecoveryRequestResponseDTO(
                request.getStatus(),
                request.getMethod(),
                request.getCreatedAt(),
                request.getExpiresAt(),
                request.getResolvedAt(),
                request.getCancelledAt()
        );
    }
}