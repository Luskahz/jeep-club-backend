package com.jeepclub.backend.authentication.api.dto.token;

import com.jeepclub.backend.authentication.core.application.result.AuthTokens;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

@Schema(
        name = "AuthTokenResponse",
        description = "Tokens emitidos após autenticação ou renovação."
)
public record AuthTokenResponseDTO(

        @Schema(
                description = "Refresh token usado para renovar a autenticação.",
                example = "8f6c1a6d-raw-refresh-token-example"
        )
        String refreshToken,

        @Schema(
                description = "Access token JWT usado nas chamadas autenticadas.",
                example = "eyJhbGciOiJIUzI1NiJ9..."
        )
        String accessToken,

        @Schema(
                description = "Tempo restante de validade do access token em segundos.",
                example = "900"
        )
        long expiresInSeconds
) {

        public static AuthTokenResponseDTO from(AuthTokens tokens) {
                Objects.requireNonNull(tokens, "tokens cannot be null");

                return new AuthTokenResponseDTO(
                        tokens.refreshToken(),
                        tokens.accessToken(),
                        tokens.expiresInSeconds()
                );
        }
}