package com.jeepclub.backend.authentication.core.services;

import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.dtos.UserPayload;

public interface TokenService {
    String generateAccessToken(User domainUser);
    String generateRefreshToken(User domainUser);
    boolean validateToken(String token);
    UserPayload getPayload(String token);
}
