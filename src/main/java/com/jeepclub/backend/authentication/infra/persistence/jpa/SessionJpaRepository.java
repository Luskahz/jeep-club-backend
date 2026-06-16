package com.jeepclub.backend.authentication.infra.persistence.jpa;

import com.jeepclub.backend.authentication.core.domain.enums.SessionStatus;
import com.jeepclub.backend.authentication.infra.persistence.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionJpaRepository
        extends JpaRepository<SessionEntity, Long> {

    Optional<SessionEntity> findFirstByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId,
            SessionStatus status
    );

    List<SessionEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}