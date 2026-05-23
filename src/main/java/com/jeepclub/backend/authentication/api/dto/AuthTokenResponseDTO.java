package com.jeepclub.backend.authentication.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tokens de autenticação gerados para o usuário autenticado.")
public record AuthTokenResponseDTO(

        @Schema(
                description = "Refresh token utilizado para renovar a sessão.",
                example = "eyJhbGciOiJIUzI1NiJ9.refresh-token-example",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String refreshToken,

        @Schema(
                description = "Access token JWT utilizado para autenticação das requisições.",
                example = "eyJhbGciOiJIUzI1NiJ9.access-token-example",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String accessToken,

        @Schema(
                description = "Tempo restante de validade do access token, em segundos.",
                example = "900",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        long expiresInSeconds
) {
}