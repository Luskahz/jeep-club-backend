package com.jeepclub.backend.authentication.api.dto.me;

public record AuthMeResponseDTO(
        Long userId,
        Long sessionId,
        boolean sessionActive,
        long expiresInSeconds
) {}