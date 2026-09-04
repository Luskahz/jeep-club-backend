package com.jeepclub.backend.identity.api.module;

public record UserAuthenticationTokens(
        String refreshToken,
        String accessToken,
        long expiresInSeconds
) {
}
