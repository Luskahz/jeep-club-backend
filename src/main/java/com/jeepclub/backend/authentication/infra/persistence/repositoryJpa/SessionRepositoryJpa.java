package com.jeepclub.backend.authentication.infra.persistence.repositoryJpa;

import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.domain.model.SessionStatus;
import com.jeepclub.backend.authentication.core.repositories.SessionRepository;
import com.jeepclub.backend.authentication.infra.persistence.jpa.SessionJpaRepository;
import com.jeepclub.backend.authentication.infra.persistence.mapper.SessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SessionRepositoryJpa implements SessionRepository {

    private final SessionJpaRepository jpaRepository;

    @Override
    public Session save(Session session) {
        return SessionMapper.toDomain(jpaRepository.save(SessionMapper.toEntity(session)));
    }

    @Override
    public Optional<Session> findActiveByUserId(Long userId) {
        return jpaRepository.findByUserIdAndStatus(userId, SessionStatus.ACTIVE)
                .map(SessionMapper::toDomain);
    }

    @Override
    public Optional<Session> findByRefreshToken(String refreshToken) {
        return jpaRepository.findByRefreshToken(refreshToken)
                .map(SessionMapper::toDomain);
    }
}
