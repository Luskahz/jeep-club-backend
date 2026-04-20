package com.jeepclub.backend.authentication.api.dtos;

public record AuthTokenResponseDTO(
        String refreshToken,
        String accessToken,
        long expiresInSeconds
) {}
