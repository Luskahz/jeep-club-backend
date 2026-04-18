package com.jeepclub.backend.authentication.api.dtos.me;

public record AuthMeResponseDTO(
        Long userId,
        String sessionId,
        Long expiresInSeconds
) {
}