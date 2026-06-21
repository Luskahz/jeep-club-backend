package com.jeepclub.backend.authentication.core.repository;

import com.jeepclub.backend.authentication.core.domain.model.Session;

import java.util.List;
import java.util.Optional;

public interface SessionRepository {

    Session save(Session session);

    List<Session> findAll();

    Optional<Session> findById(Long id);

    List<Session> findByUserId(Long userId);

    Optional<Session> findActiveByUserId(Long userId);
}