package com.jeepclub.backend.authentication.core.application.services;

import com.jeepclub.backend.authentication.core.application.results.LogoutResult;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.repositories.SessionRepository;
import com.jeepclub.backend.authentication.core.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogoutService {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;

    @Transactional
    public LogoutResult logout(Long userId) {
        Session session = sessionRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Sessão não encontrada"));

        userRepository.findById(session.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        session.revoke();
        sessionRepository.save(session);

        return new LogoutResult("Logout realizado com sucesso");
    }
}