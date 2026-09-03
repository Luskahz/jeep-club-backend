package com.jeepclub.backend.authentication.core.application.service.refreshtoken;

import com.jeepclub.backend.authentication.core.application.exceptions.refreshtoken.RefreshTokenInvalidException;
import com.jeepclub.backend.authentication.core.application.result.AuthTokens;
import com.jeepclub.backend.authentication.core.application.result.IssuedRefreshToken;
import com.jeepclub.backend.authentication.core.application.service.internal.RefreshTokenIssuanceService;
import com.jeepclub.backend.authentication.core.domain.model.IssuedAccessToken;
import com.jeepclub.backend.authentication.core.domain.model.RefreshToken;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.port.JwtService;
import com.jeepclub.backend.authentication.core.port.RefreshTokenHashService;
import com.jeepclub.backend.authentication.core.repository.RefreshTokenRepository;
import com.jeepclub.backend.authentication.core.repository.SessionRepository;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final RefreshTokenHashService tokenHashService;
    private final RefreshTokenIssuanceService refreshTokenIssuanceService;
    private final JwtService jwtService;
    private final Clock clock;

    @Transactional
    public AuthTokens rotate(String rawRefreshToken) {
        Instant now = Instant.now(clock);

        String tokenHash = tokenHashService.hash(rawRefreshToken);

        Long sessionId = refreshTokenRepository
                .findSessionIdByTokenHash(tokenHash)
                .orElseThrow(RefreshTokenInvalidException::new);

        Long userId = sessionRepository
                .findUserIdById(sessionId)
                .orElseThrow(RefreshTokenInvalidException::new);

        User user = userRepository
                .findByIdForUpdate(userId)
                .orElseThrow(RefreshTokenInvalidException::new);

        Session session = sessionRepository
                .findByIdForUpdate(sessionId)
                .orElseThrow(RefreshTokenInvalidException::new);

        RefreshToken existingToken = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow(RefreshTokenInvalidException::new);

        if (!existingToken.getSession().getId().equals(session.getId())
                || !existingToken.isValid(now)
                || !session.isValid(now)
                || !user.isActive()) {
            throw new RefreshTokenInvalidException();
        }

        IssuedRefreshToken issuedRefreshToken =
                refreshTokenIssuanceService.issue(session, now);

        existingToken.rotate(
                issuedRefreshToken.refreshToken().getId(),
                now
        );

        refreshTokenRepository.save(existingToken);

        IssuedAccessToken issuedAccessToken =
                jwtService.generateAccessToken(user.getId(), session);

        long expiresInSeconds = Math.max(
                Duration.between(
                        now,
                        issuedAccessToken.expiresAt()
                ).getSeconds(),
                0
        );

        return new AuthTokens(
                issuedRefreshToken.rawToken(),
                issuedAccessToken.token(),
                expiresInSeconds
        );
    }
}
