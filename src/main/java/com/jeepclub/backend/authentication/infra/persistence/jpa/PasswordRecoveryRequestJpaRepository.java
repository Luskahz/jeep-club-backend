package com.jeepclub.backend.authentication.infra.persistence.jpa;

import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestStatus;
import com.jeepclub.backend.authentication.infra.persistence.entity.PasswordRecoveryRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface PasswordRecoveryRequestJpaRepository
        extends JpaRepository<PasswordRecoveryRequestEntity, Long> {

    Optional<PasswordRecoveryRequestEntity> findByTokenHash(String tokenHash);

    Optional<PasswordRecoveryRequestEntity> findFirstByUserIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            Long userId,
            PasswordRecoveryRequestStatus status,
            Instant now
    );

    Optional<PasswordRecoveryRequestEntity> findFirstByUserIdAndStatusAndMethodAndExpiresAtAfterOrderByCreatedAtDesc(
            Long userId,
            PasswordRecoveryRequestStatus status,
            PasswordRecoveryRequestMethod method,
            Instant now
    );
}