package com.jeepclub.backend.authentication.core.application.services;

import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.domain.exception.CpfNotFoundException;
import com.jeepclub.backend.authentication.core.domain.exception.InvalidPasswordException;
import com.jeepclub.backend.authentication.core.port.PasswordHasher;
import com.jeepclub.backend.authentication.core.port.TokenHashService;
import com.jeepclub.backend.authentication.core.repositories.SessionRepository;
import com.jeepclub.backend.authentication.core.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LoginService {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final TokenHashService tokenHashService;
    private final SessionRepository sessionRepository;

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
                .map(existing -> {
                    if (existing.isValid(now)) {
                        return existing;
                    }
                    return Session.create(user.getId(), newRefreshToken, refreshExpiresAt);
                })
                .orElseGet(() -> Session.create(user.getId(), newRefreshToken, refreshExpiresAt));

        sessionRepository.save(session);

        String accessToken = tokenService.generateAccessToken(user);
        long expiresInSeconds = tokenService.getAccessTokenExpiresInSeconds();

        user.recordSuccessfulLogin(now);
        userRepository.save(user);

        return new AuthTokens(newRefreshToken, accessToken, expiresInSeconds);
    }
}
