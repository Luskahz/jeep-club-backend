package com.jeepclub.backend.iam.identity.api.http.dto.user;

import com.jeepclub.backend.iam.identity.api.module.UserAuthenticationTokens;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

@Schema(name = "UserAuthenticationTokenResponse", description = "Tokens emitidos após o cadastro do usuário.")
public record UserAuthenticationTokenResponseDTO(
        @Schema(description = "Refresh token emitido para renovar a autenticação.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String refreshToken,
        @Schema(description = "Access token JWT emitido para a sessão.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String accessToken,
        @Schema(description = "Validade do access token, em segundos.", example = "900",
                requiredMode = Schema.RequiredMode.REQUIRED)
        long expiresInSeconds
) {
    public static UserAuthenticationTokenResponseDTO from(UserAuthenticationTokens tokens) {
        Objects.requireNonNull(tokens, "tokens cannot be null");
        return new UserAuthenticationTokenResponseDTO(
                tokens.refreshToken(), tokens.accessToken(), tokens.expiresInSeconds()
        );
    }
}
