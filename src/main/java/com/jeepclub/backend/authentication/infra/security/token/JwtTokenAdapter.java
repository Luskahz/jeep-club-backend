package com.jeepclub.backend.authentication.infra.security.token;

import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.dtos.UserPayload;
import com.jeepclub.backend.authentication.core.services.TokenService;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenAdapter implements TokenService {


    @Override
    public String generateAccessToken(User domainUser) {
        return "";
    }

    @Override
    public String generateRefreshToken(User domainUser) {
        return "";
    }

    @Override
    public boolean validateToken(String token) {
        return false;
    }

    @Override
    public UserPayload getPayload(String token) {
        return null;
    }

}
