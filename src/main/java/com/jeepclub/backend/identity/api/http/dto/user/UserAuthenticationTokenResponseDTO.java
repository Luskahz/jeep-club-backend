package com.jeepclub.backend.identity.api.http.dto.user;

import com.jeepclub.backend.identity.api.module.UserAuthenticationTokens;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

@Schema(name = "UserAuthenticationTokenResponse", description = "Tokens emitidos após o cadastro do usuário.")
public record UserAuthenticationTokenResponseDTO(
        String refreshToken,
        String accessToken,
        long expiresInSeconds
) {
    public static UserAuthenticationTokenResponseDTO from(UserAuthenticationTokens tokens) {
        Objects.requireNonNull(tokens, "tokens cannot be null");
        return new UserAuthenticationTokenResponseDTO(
                tokens.refreshToken(), tokens.accessToken(), tokens.expiresInSeconds()
        );
    }
}
