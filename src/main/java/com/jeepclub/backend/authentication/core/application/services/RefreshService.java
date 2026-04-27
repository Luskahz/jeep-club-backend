package com.jeepclub.backend.authentication.core.application.services;

import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.domain.model.IssuedAccessToken;
import com.jeepclub.backend.authentication.core.domain.model.RefreshToken;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.port.AuthTimeProperties;
import com.jeepclub.backend.authentication.core.port.JwtService;
import com.jeepclub.backend.authentication.core.port.RefreshTokenGenerator;
import com.jeepclub.backend.authentication.core.port.RefreshTokenHashService;
import com.jeepclub.backend.authentication.core.repository.RefreshTokenRepository;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RefreshService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final RefreshTokenHashService tokenHashService;
    private final RefreshTokenGenerator tokenGenerator;
    private final JwtService jwtService;
    private final AuthTimeProperties authTimeProperties;

    @Transactional
    public AuthTokens refresh(String rawRefreshToken) {
        Instant now = Instant.now();

        String tokenHash = tokenHashService.hash(rawRefreshToken);

        RefreshToken existingToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new SecurityException("Refresh token inválido"));

        if (!existingToken.isValid(now)) {
            throw new SecurityException("Refresh token invalido para as regras de rotação");
        }

        User user = userRepository.findById(existingToken.getSession().getUserId())
                .orElseThrow(() -> new SecurityException("Usuário não encontrado para este token"));

        String newRawToken = tokenGenerator.generate();
        String newTokenHash = tokenHashService.hash(newRawToken);

        RefreshToken newToken = RefreshToken.create(
                existingToken.getSession(),
                newTokenHash,
                authTimeProperties.refreshTokenTtl()
        );
        RefreshToken savedNewToken = refreshTokenRepository.save(newToken, existingToken.getSession());
        existingToken.rotate(savedNewToken.getId(), now);
        refreshTokenRepository.save(existingToken, existingToken.getSession());

        IssuedAccessToken issuedAccessToken = jwtService.generateAccessToken(user, existingToken.getSession());
        long expiresInSeconds = Math.max(
                Duration.between(now, issuedAccessToken.expiresAt()).getSeconds(), 0
        );

        return new AuthTokens(newRawToken, issuedAccessToken.token(), expiresInSeconds);
    }
}