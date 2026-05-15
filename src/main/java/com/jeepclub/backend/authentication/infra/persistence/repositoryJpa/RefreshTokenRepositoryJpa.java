package com.jeepclub.backend.authentication.infra.persistence.repositoryJpa;

import com.jeepclub.backend.authentication.core.domain.enums.RefreshTokenStatus;
import com.jeepclub.backend.authentication.core.domain.model.RefreshToken;
import com.jeepclub.backend.authentication.core.domain.model.Session;
import com.jeepclub.backend.authentication.core.repository.RefreshTokenRepository;
import com.jeepclub.backend.authentication.infra.persistence.jpa.RefreshTokenJpaRepository;
import com.jeepclub.backend.authentication.infra.persistence.jpa.SessionJpaRepository;
import com.jeepclub.backend.authentication.infra.persistence.mapper.RefreshTokenMapper;
import com.jeepclub.backend.authentication.infra.persistence.mapper.SessionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepositoryJpa implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;
    private final SessionJpaRepository sessionJpa;

    @Override
    public RefreshToken save(RefreshToken token, Session session) {
        var entity = jpa.findBySessionId(session.getId())
                .map(e -> {
                    e.setTokenHash(token.getTokenHash());
                    e.setExpiresAt(token.getExpiresAt());
                    e.setStatus(token.getStatus());
                    e.setReplacedByTokenId(token.getReplacedByTokenId());
                    return e;
                })
                .orElseGet(() -> RefreshTokenMapper.toEntity(token));

        var saved = jpa.save(entity);
        return RefreshTokenMapper.toDomain(saved, session);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash)
                .flatMap(entity ->
                        sessionJpa.findById(entity.getSessionId())
                                .map(sessionEntity ->
                                        RefreshTokenMapper.toDomain(entity, SessionMapper.toDomain(sessionEntity))
                                )
                );
    }

}