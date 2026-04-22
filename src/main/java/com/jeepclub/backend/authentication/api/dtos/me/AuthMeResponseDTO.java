package com.jeepclub.backend.authentication.api.dtos.me;

public record AuthMeResponseDTO(
        Long userId,
        Long sessionId,
        boolean sessionActive,
        long expiresInSeconds
) {}