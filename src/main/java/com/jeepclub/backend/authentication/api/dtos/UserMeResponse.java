package com.jeepclub.backend.authentication.api.dtos;

public record UserMeResponse(
        String userId,
        String sessionId,
        Long expiresInSeconds // ou String se preferir um formato de data/hora
) {}