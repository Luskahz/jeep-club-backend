package com.jeepclub.backend.authentication.api.dtos;

public record AuthMeResponseDTO(
        Long userId,
        String sessionId,
        Long expiresInSeconds
) {
}