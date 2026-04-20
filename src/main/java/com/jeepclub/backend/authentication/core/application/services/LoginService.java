package com.jeepclub.backend.authentication.core.application.services;

import com.jeepclub.backend.authentication.core.application.results.AuthTokens;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.domain.exception.CpfNotFoundException;
import com.jeepclub.backend.authentication.core.domain.exception.InvalidPasswordException;
import com.jeepclub.backend.authentication.core.repositories.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import java.time.Instant;

@Service
public class LoginService {
    private final UserRepository userRepository;

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

        String newRefreshToken = tokenService.generateRefreshToken(user);
        Instant refreshExpiresAt = now.plusSeconds(tokenService.getRefreshTokenExpiresInSeconds());

        // stateful: reutiliza sessão ativa ou cria uma nova
        Session session = sessionRepository.findActiveByUserId(user.getId())
                .map(existing -> {
                    if (existing.isValid(now)) {
                        existing.renew(newRefreshToken, refreshExpiresAt);
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
