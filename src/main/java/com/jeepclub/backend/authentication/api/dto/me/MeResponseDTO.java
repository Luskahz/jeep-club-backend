package com.jeepclub.backend.authentication.api.dto.me;

public record MeResponseDTO(
        Long userId,
        Long sessionId,
        boolean sessionActive,
        long expiresInSeconds
) {}