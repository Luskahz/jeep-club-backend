package com.jeepclub.backend.authentication.api.http.dto.session;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jeepclub.backend.authentication.core.application.result.login.AuthenticatedLoginResult;
import com.jeepclub.backend.authentication.core.application.result.login.LoginResult;
import com.jeepclub.backend.authentication.core.application.result.login.PasswordChangeRequiredLoginResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Resposta do fluxo de login.")
public record LoginResponseDTO(

        @Schema(
                description = "Status do login.",
                example = "AUTHENTICATED",
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"AUTHENTICATED", "PASSWORD_CHANGE_REQUIRED"}
        )
        String status,

        @Schema(
                description = "Refresh token emitido quando o login é concluído.",
                example = "eyJhbGciOiJIUzI1NiJ9...",
                nullable = true
        )
        String refreshToken,

        @Schema(
                description = "Access token emitido quando o login é concluído.",
                example = "eyJhbGciOiJIUzI1NiJ9...",
                nullable = true
        )
        String accessToken,

        @Schema(
                description = "Tempo de expiração do access token em segundos.",
                example = "3600",
                nullable = true
        )
        Long expiresInSeconds,

        @Schema(
                description = "Token temporário usado para concluir troca obrigatória de senha.",
                example = "u9Tt8sGv2xYpQz...",
                nullable = true
        )
        String passwordChangeToken,

        @Schema(
                description = "Data de expiração do token temporário de troca de senha.",
                example = "2026-05-21T20:30:00Z",
                nullable = true
        )
        Instant passwordChangeTokenExpiresAt
) {

    public static LoginResponseDTO from(LoginResult result) {
        Objects.requireNonNull(result, "result cannot be null");

        if (result instanceof AuthenticatedLoginResult authenticated) {
            return new LoginResponseDTO(
                    "AUTHENTICATED",
                    authenticated.tokens().refreshToken(),
                    authenticated.tokens().accessToken(),
                    authenticated.tokens().expiresInSeconds(),
                    null,
                    null
            );
        }

        if (result instanceof PasswordChangeRequiredLoginResult passwordChangeRequired) {
            return new LoginResponseDTO(
                    "PASSWORD_CHANGE_REQUIRED",
                    null,
                    null,
                    null,
                    passwordChangeRequired.passwordChangeToken(),
                    passwordChangeRequired.expiresAt()
            );
        }

        throw new IllegalArgumentException("Unsupported login result type.");
    }
}