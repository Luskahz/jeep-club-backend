package com.jeepclub.backend.authentication.core.application.services;

import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.authentication.core.repository.SessionRepository;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AccessTokenAuthenticationService {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    public void validate(Long userId, Long sessionId) {
        Instant now = Instant.now();

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Sessão não encontrada"));

        if (!session.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Sessão não pertence ao usuário");
        }

        if (!session.isValid(now)) {
            throw new IllegalArgumentException("Sessão inválida, expirada ou encerrada");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        if (!user.isActive()) {
            throw new IllegalArgumentException("Usuário inativo");
        }
    }
}