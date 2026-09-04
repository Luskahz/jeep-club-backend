package com.jeepclub.backend.iam.authentication.api.http.dto.recovery;

import com.jeepclub.backend.iam.authentication.core.application.result.TemporaryPasswordAdminResult;
import com.jeepclub.backend.iam.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.iam.authentication.core.domain.enums.PasswordRecoveryRequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Objects;

@Schema(description = "Resposta administrativa contendo senha provisória e dados da solicitação de recuperação.")
public record TemporaryPasswordAdminResponseDTO(

        @Schema(
                description = "Senha provisória gerada para o usuário.",
                example = "A9x@72Lm#pQ",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String temporaryPassword,

        @Schema(
                description = "Status atual da solicitação de recuperação.",
                example = "OPEN",
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"OPEN", "RESOLVED", "CANCELLED"}
        )
        PasswordRecoveryRequestStatus status,

        @Schema(
                description = "Método definido para recuperação de senha.",
                example = "ADMIN_TEMPORARY_PASSWORD",
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
                example = "2026-05-27T18:30:00Z",
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

    public static TemporaryPasswordAdminResponseDTO from(
            TemporaryPasswordAdminResult result
    ) {
        Objects.requireNonNull(result, "result cannot be null");
        Objects.requireNonNull(result.recoveryRequest(), "recoveryRequest cannot be null");

        return new TemporaryPasswordAdminResponseDTO(
                result.temporaryPassword(),
                result.recoveryRequest().getStatus(),
                result.recoveryRequest().getMethod(),
                result.recoveryRequest().getCreatedAt(),
                result.recoveryRequest().getExpiresAt(),
                result.recoveryRequest().getResolvedAt(),
                result.recoveryRequest().getCancelledAt()
        );
    }
}