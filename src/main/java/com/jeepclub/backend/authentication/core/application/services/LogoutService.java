package com.jeepclub.backend.authentication.core.application.services;

import com.jeepclub.backend.authentication.core.application.results.LogoutResult;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.repository.SessionRepository;
import com.jeepclub.backend.authentication.core.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    @Transactional
    public LogoutResult logout(Long userId) {
        Instant now = Instant.now();
        Session session = sessionRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Sessão não encontrada"));

        userRepository.findById(session.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        session.logout(now);
        sessionRepository.save(session);

        return new LogoutResult("Logout realizado com sucesso");
    }
}