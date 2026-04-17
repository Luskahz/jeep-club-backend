package com.jeepclub.backend.authentication.core.services;

import com.jeepclub.backend.authentication.core.domain.model.User;

public interface TokenService {
    String generateAccessToken(User user);
    String generateRefreshToken(User user);
    long getAccessTokenExpiresInSeconds();
    boolean validateToken(String token);
    Long getUserIdFromToken(String token);
}
