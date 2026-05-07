package com.jeepclub.backend.authentication.api.dto.me;

import java.util.List;

public record MeResponseDTO(
        Long userId,
        Long sessionId,
        boolean sessionActive,
        long expiresInSeconds,
        List<String> authorities
) {
}