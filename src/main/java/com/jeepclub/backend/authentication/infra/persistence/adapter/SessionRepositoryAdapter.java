package com.jeepclub.backend.authentication.infra.persistence.adapter;

import com.jeepclub.backend.authentication.core.domain.enums.SessionStatus;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.repository.SessionRepository;
import com.jeepclub.backend.authentication.infra.persistence.jpa.SessionJpaRepository;
import com.jeepclub.backend.authentication.infra.persistence.mapper.SessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class SessionRepositoryAdapter
        implements SessionRepository {

    private final SessionJpaRepository jpaRepository;

    @Override
    public Session save(Session session) {
        return SessionMapper.toDomain(
                jpaRepository.save(
                        SessionMapper.toEntity(session)
                )
        );
    }

    @Override
    public Optional<Session> findById(Long sessionId) {
        return jpaRepository.findById(sessionId)
                .map(SessionMapper::toDomain);
    }

    @Override
    public Optional<Session> findByIdForUpdate(Long sessionId) {
        return jpaRepository.findByIdForUpdate(sessionId)
                .map(SessionMapper::toDomain);
    }

    @Override
    public Optional<Long> findUserIdById(Long sessionId) {
        return jpaRepository.findUserIdById(sessionId);
    }

    @Override
    public Optional<Session> findActiveByUserId(Long userId) {
        return jpaRepository
                .findFirstByUserIdAndStatusOrderByCreatedAtDesc(
                        userId,
                        SessionStatus.ACTIVE
                )
                .map(SessionMapper::toDomain);
    }

    @Override
    public Optional<Session> findActiveByUserIdForUpdate(
            Long userId
    ) {
        return jpaRepository
                .findTopByUserIdAndStatusOrderByCreatedAtDesc(
                        userId,
                        SessionStatus.ACTIVE
                )
                .map(SessionMapper::toDomain);
    }

    @Override
    public List<Session> findAll() {
        return jpaRepository.findAll()
                .stream()
                .map(SessionMapper::toDomain)
                .toList();
    }

    @Override
    public List<Session> findByUserId(Long userId) {
        return jpaRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(SessionMapper::toDomain)
                .toList();
    }

    @Override
    public void revokeActiveByUserId(Long userId) {
        jpaRepository.revokeActiveByUserId(userId);
    }
}
