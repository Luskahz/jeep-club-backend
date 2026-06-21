package com.jeepclub.backend.authentication.infra.persistence.adapter;

import com.jeepclub.backend.authentication.core.domain.model.RefreshToken;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.repository.RefreshTokenRepository;
import com.jeepclub.backend.authentication.infra.persistence.entity.RefreshTokenEntity;
import com.jeepclub.backend.authentication.infra.persistence.jpa.RefreshTokenJpaRepository;
import com.jeepclub.backend.authentication.infra.persistence.jpa.SessionJpaRepository;
import com.jeepclub.backend.authentication.infra.persistence.mapper.RefreshTokenMapper;
import com.jeepclub.backend.authentication.infra.persistence.mapper.SessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryAdapter
        implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;
    private final SessionJpaRepository sessionJpaRepository;

    @Override
    public RefreshToken save(RefreshToken token) {
        validateTokenSession(token);

        RefreshTokenEntity entity =
                RefreshTokenMapper.toEntity(token);

        RefreshTokenEntity savedEntity =
                jpaRepository.save(entity);

        return RefreshTokenMapper.toDomain(
                savedEntity,
                token.getSession()
        );
    }

    @Override
    public List<RefreshToken> findAll() {
        List<RefreshTokenEntity> tokenEntities =
                jpaRepository.findAll();

        return mapToDomains(tokenEntities);
    }

    @Override
    public Optional<RefreshToken> findById(Long id) {
        return jpaRepository.findById(id)
                .flatMap(this::mapToDomain);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash)
                .flatMap(this::mapToDomain);
    }

    @Override
    public List<RefreshToken> findByUserId(Long userId) {
        List<Session> sessions =
                sessionJpaRepository
                        .findByUserIdOrderByCreatedAtDesc(userId)
                        .stream()
                        .map(SessionMapper::toDomain)
                        .toList();

        if (sessions.isEmpty()) {
            return List.of();
        }

        List<Long> sessionIds = sessions.stream()
                .map(Session::getId)
                .toList();

        List<RefreshTokenEntity> tokenEntities =
                jpaRepository
                        .findBySessionIdInOrderByCreatedAtDesc(
                                sessionIds
                        );

        Map<Long, Session> sessionsById = sessions.stream()
                .collect(Collectors.toMap(
                        Session::getId,
                        Function.identity()
                ));

        return tokenEntities.stream()
                .map(entity -> mapToDomain(
                        entity,
                        sessionsById
                ))
                .toList();
    }

    private Optional<RefreshToken> mapToDomain(
            RefreshTokenEntity entity
    ) {
        return sessionJpaRepository
                .findById(entity.getSessionId())
                .map(SessionMapper::toDomain)
                .map(session ->
                        RefreshTokenMapper.toDomain(
                                entity,
                                session
                        )
                );
    }

    private List<RefreshToken> mapToDomains(
            Collection<RefreshTokenEntity> tokenEntities
    ) {
        if (tokenEntities.isEmpty()) {
            return List.of();
        }

        List<Long> sessionIds = tokenEntities.stream()
                .map(RefreshTokenEntity::getSessionId)
                .distinct()
                .toList();

        Map<Long, Session> sessionsById =
                sessionJpaRepository.findAllById(sessionIds)
                        .stream()
                        .map(SessionMapper::toDomain)
                        .collect(Collectors.toMap(
                                Session::getId,
                                Function.identity()
                        ));

        return tokenEntities.stream()
                .map(entity -> mapToDomain(
                        entity,
                        sessionsById
                ))
                .toList();
    }

    private RefreshToken mapToDomain(
            RefreshTokenEntity entity,
            Map<Long, Session> sessionsById
    ) {
        Session session =
                sessionsById.get(entity.getSessionId());

        if (session == null) {
            throw new IllegalStateException(
                    "Session not found for refresh token id: "
                            + entity.getId()
            );
        }

        return RefreshTokenMapper.toDomain(
                entity,
                session
        );
    }

    private void validateTokenSession(RefreshToken token) {
        if (token == null) {
            throw new IllegalArgumentException(
                    "Refresh token is required."
            );
        }

        if (token.getSession() == null) {
            throw new IllegalArgumentException(
                    "Refresh token session is required."
            );
        }

        if (token.getSession().getId() == null) {
            throw new IllegalArgumentException(
                    "Refresh token session id is required."
            );
        }
    }
}