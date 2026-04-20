package com.jeepclub.backend.authentication.core.application.services;

import com.jeepclub.backend.authentication.core.application.results.MeResult;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.authentication.core.port.TokenService;
import com.jeepclub.backend.authentication.core.repositories.SessionRepository;
import com.jeepclub.backend.authentication.core.repositories.UserRepository;
import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.application.results.IssuedRefreshToken;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;


/**
 * Decodifica o token e retorna as informações da sessão do usuário.
 * (Task BACK-XX: Obter usuário autenticado /me)
 */
@Service
public class MeService {
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;

    @Transactional
    public MeResult me(
            @NotNull Long userId,
            @NotNull Long sessionId,
            Instant accessTokenExpiresAt
    ) {
        Instant now = Instant.now();
        RefreshToken existingToken = findRefreshTokenByToken(refreshToken)
                .orElseThrow(() -> new SecurityException("Invalid refresh token"));
        if (!existingToken.isValid(now)) {
            throw new SecurityException("Refresh token invalid");
        }

        User user = userRepository.findById(existingToken.getSession().getUserId())
                .orElseThrow(() -> new SecurityException("User not found in session"));

        IssuedRefreshToken issuedRefreshToken = rotateRefreshTokenFromExisting(existingToken, now);
        IssuedAccessToken issuedAccessToken = jwtService.generateAccessToken(user, issuedRefreshToken.refreshToken().getSession());
        long expiresInSeconds = Math.max(Duration.between(now, issuedAccessToken.expiresAt()).getSeconds(), 0);
        return new AuthTokens(
                issuedRefreshToken.rawToken(),
                issuedAccessToken.token(),
                expiresInSeconds
        );
    }
}
