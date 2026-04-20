package com.jeepclub.backend.authentication.core.application.results;

public record IssuedRefreshToken(
        RefreshToken refreshToken,
        String rawToken
) {
}
