package com.jeepclub.backend.authentication.core.application.services;

import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.application.results.IssuedRefreshToken;
import com.jeepclub.backend.authentication.core.domain.exception.CpfNotFoundException;
import com.jeepclub.backend.authentication.core.domain.exception.InvalidPasswordException;
import com.jeepclub.backend.authentication.core.domain.model.IssuedAccessToken;
import com.jeepclub.backend.authentication.core.domain.model.RefreshToken;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.port.AuthTimeProperties;
import com.jeepclub.backend.authentication.core.port.JwtService;
import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.authentication.core.port.TokenGenerator;
import com.jeepclub.backend.authentication.core.port.TokenHashService;
import com.jeepclub.backend.authentication.core.repositories.RefreshTokenRepository;
import com.jeepclub.backend.authentication.core.repositories.SessionRepository;
import com.jeepclub.backend.authentication.core.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final TokenHashService tokenHashService;
    private final TokenGenerator tokenGenerator;
    private final JwtService jwtService;
    private final AuthTimeProperties authTimeProperties;

    @Transactional
    public AuthTokens login(String cpf, String senha) {
        Instant now = Instant.now();

        User user = userRepository.findByCpf(cpf)
                .orElseThrow(() -> new CpfNotFoundException("CPF não encontrado"));

        if (!passwordHasher.matches(senha, user.getPasswordHash())) {
            user.registerFailedLogin();
            userRepository.save(user);
            throw new InvalidPasswordException();
        }

        Session session = sessionRepository.findActiveByUserId(user.getId())
                .filter(existing -> existing.isValid(now))
                .orElseGet(() -> sessionRepository.save(
                        Session.create(user.getId(), authTimeProperties.sessionTtl())
                ));

        String rawToken = tokenGenerator.generate();
        String tokenHash = tokenHashService.hash(rawToken);

        RefreshToken refreshToken = RefreshToken.create(
                session,
                tokenHash,
                authTimeProperties.refreshTokenTtl()
        );
        refreshTokenRepository.save(refreshToken, session);

        IssuedAccessToken issuedAccessToken = jwtService.generateAccessToken(user, session);
        long expiresInSeconds = Math.max(
                Duration.between(now, issuedAccessToken.expiresAt()).getSeconds(), 0
        );

        user.recordSuccessfulLogin(now);
        userRepository.save(user);

        return new AuthTokens(rawToken, issuedAccessToken.token(), expiresInSeconds);
    }
}