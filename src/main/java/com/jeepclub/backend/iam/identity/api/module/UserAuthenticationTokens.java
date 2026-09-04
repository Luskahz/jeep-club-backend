package com.jeepclub.backend.iam.identity.api.module;

public record UserAuthenticationTokens(
        String refreshToken,
        String accessToken,
        long expiresInSeconds
) {
}
