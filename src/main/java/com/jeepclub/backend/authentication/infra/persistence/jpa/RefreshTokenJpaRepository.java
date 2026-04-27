package com.jeepclub.backend.authentication.infra.persistence.jpa;

import com.jeepclub.backend.authentication.core.domain.enums.RefreshTokenStatus;
import com.jeepclub.backend.authentication.infra.persistence.entities.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    Optional<RefreshTokenEntity> findBySessionIdAndStatus(Long sessionId, RefreshTokenStatus status);

    Optional<RefreshTokenEntity> findBySessionId(Long sessionId);
}