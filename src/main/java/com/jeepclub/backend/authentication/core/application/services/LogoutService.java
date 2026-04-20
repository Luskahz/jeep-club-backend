package com.jeepclub.backend.authentication.core.application.services;

import com.jeepclub.backend.authentication.core.application.results.LogoutResult;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class LogoutService {

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
