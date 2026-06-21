package com.jeepclub.backend.authentication.infra.persistence.jpa;

import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestMethod;
import com.jeepclub.backend.authentication.core.domain.enums.PasswordRecoveryRequestStatus;
import com.jeepclub.backend.authentication.infra.persistence.entity.PasswordRecoveryRequestEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PasswordRecoveryRequestJpaRepository
        extends JpaRepository<PasswordRecoveryRequestEntity, Long> {

    Optional<PasswordRecoveryRequestEntity> findByTokenHash(String tokenHash);

    List<PasswordRecoveryRequestEntity> findByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    Optional<PasswordRecoveryRequestEntity>
    findFirstByUserIdAndStatusAndExpiresAtAfterOrderByCreatedAtDesc(
            Long userId,
            PasswordRecoveryRequestStatus status,
            Instant now
    );

    Optional<PasswordRecoveryRequestEntity>
    findFirstByUserIdAndStatusAndMethodAndExpiresAtAfterOrderByCreatedAtDesc(
            Long userId,
            PasswordRecoveryRequestStatus status,
            PasswordRecoveryRequestMethod method,
            Instant now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT request
        FROM PasswordRecoveryRequestEntity request
        WHERE request.tokenHash = :tokenHash
        """)
    Optional<PasswordRecoveryRequestEntity> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );
}