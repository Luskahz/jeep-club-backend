package com.jeepclub.backend.authentication.core.application.services;

import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.dtos.UserPayload;

public interface TokenService {
    String generateAccessToken(User user);
    String generateRefreshToken(User user);
    long getAccessTokenExpiresInSeconds();
    long getRefreshTokenExpiresInSeconds();
    boolean validateToken(String token);
    UserPayload getPayload(String token);
    Long getUserIdFromToken(String token);
}
