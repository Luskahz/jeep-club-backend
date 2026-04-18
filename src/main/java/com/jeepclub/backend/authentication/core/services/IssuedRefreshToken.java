package com.jeepclub.backend.authentication.core.services;

public record IssuedRefreshToken(
        RefreshToken refreshToken,
        String rawToken
) {
}
