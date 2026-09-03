package com.jeepclub.backend.authentication.core.application.service.internal;

import com.jeepclub.backend.authentication.core.application.result.AuthTokens;
import com.jeepclub.backend.authentication.core.application.result.IssuedRefreshToken;
import com.jeepclub.backend.authentication.core.domain.model.IssuedAccessToken;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.port.JwtService;
import com.jeepclub.backend.authentication.core.port.ApplicationTimeProperties;
import com.jeepclub.backend.authentication.core.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class TokenIssuanceService {

    private final SessionRepository sessionRepository;
    private final RefreshTokenIssuanceService refreshTokenIssuanceService;
    private final JwtService jwtService;
    private final ApplicationTimeProperties authTimeProperties;

    public AuthTokens issue(User user, Instant now) {
        user.assertCanAuthenticate();

        Session session = sessionRepository
                .findActiveByUserIdForUpdate(user.getId())
                .filter(existing -> existing.isValid(now))
                .orElseGet(() -> sessionRepository.save(
                        Session.create(user.getId(), authTimeProperties.sessionTtl(), now)
                ));

        IssuedRefreshToken refreshToken = refreshTokenIssuanceService.issue(session, now);

        IssuedAccessToken accessToken = jwtService.generateAccessToken(user.getId(), session);
        long expiresInSeconds = Math.max(
                Duration.between(now, accessToken.expiresAt()).getSeconds(),
                0
        );
        return new AuthTokens(refreshToken.rawToken(), accessToken.token(), expiresInSeconds);
    }
}
