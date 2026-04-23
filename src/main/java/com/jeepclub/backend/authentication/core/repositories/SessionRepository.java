package com.jeepclub.backend.authentication.core.repositories;

import com.jeepclub.backend.authentication.core.domain.model.Session;

import java.util.Optional;

public interface SessionRepository {
    Session save(Session session);
    Optional<Session> findById(Long id);
    Optional<Session> findActiveByUserId(Long userId);
    Optional<Session> findByRefreshToken(String refreshToken);
}
