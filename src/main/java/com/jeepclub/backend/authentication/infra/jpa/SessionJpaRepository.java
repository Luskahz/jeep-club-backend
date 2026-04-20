package com.jeepclub.backend.authentication.infra.jpa;

import com.jeepclub.backend.authentication.core.domain.model.SessionStatus;
import com.jeepclub.backend.authentication.infra.entities.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SessionJpaRepository extends JpaRepository<SessionEntity, Long> {
    Optional<SessionEntity> findByUserIdAndStatus(Long userId, SessionStatus status);
    Optional<SessionEntity> findByRefreshToken(String refreshToken);
}
