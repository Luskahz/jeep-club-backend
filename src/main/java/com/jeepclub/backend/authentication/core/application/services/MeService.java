package com.jeepclub.backend.authentication.core.application.services;

import com.jeepclub.backend.authentication.core.application.results.MeResult;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.repositories.SessionRepository;
import com.jeepclub.backend.authentication.core.repositories.UserRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MeService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;

    @Transactional
    public MeResult me(
            @NotNull Long userId,
            @NotNull Long sessionId,
            Instant accessTokenExpiresAt
    ) {
        Instant now = Instant.now();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SecurityException("Usuário não encontrado"));

        Session session = sessionRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new SecurityException("Sessão não encontrada"));

        long expiresInSeconds = Math.max(
                Duration.between(now, accessTokenExpiresAt).getSeconds(), 0
        );

        return new MeResult(
                user.getId(),
                sessionId,
                session.isValid(now),
                expiresInSeconds
        );
    }
}