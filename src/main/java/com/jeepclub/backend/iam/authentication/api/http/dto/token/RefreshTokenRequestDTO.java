package com.jeepclub.backend.iam.authentication.api.http.dto.token;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Dados necessários para renovar os tokens de autenticação.")
public record RefreshTokenRequestDTO(

        @Schema(
                description = "Refresh token válido do usuário autenticado.",
                example = "eyJhbGciOiJIUzI1NiJ9.refresh-token-example",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "Refresh token é obrigatório.")
        String refreshToken
) {
}