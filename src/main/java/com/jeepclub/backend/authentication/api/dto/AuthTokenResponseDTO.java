package com.jeepclub.backend.authentication.api.dto;

public record AuthTokenResponseDTO(
        String refreshToken,
        String accessToken,
        long expiresInSeconds
) {}
