package com.jeepclub.backend.iam.authentication.infra.persistence.jpa;

import com.jeepclub.backend.iam.authentication.core.domain.enums.SessionStatus;
import com.jeepclub.backend.iam.authentication.infra.persistence.entity.SessionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SessionJpaRepository
        extends JpaRepository<SessionEntity, Long> {

    Optional<SessionEntity>
    findFirstByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId,
            SessionStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SessionEntity>
    findTopByUserIdAndStatusOrderByCreatedAtDesc(
            Long userId,
            SessionStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT session
            FROM SessionEntity session
            WHERE session.id = :id
            """)
    Optional<SessionEntity> findByIdForUpdate(
            @Param("id") Long id
    );

    @Query("""
            SELECT session.userId
            FROM SessionEntity session
            WHERE session.id = :id
            """)
    Optional<Long> findUserIdById(
            @Param("id") Long id
    );

    List<SessionEntity> findByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE SessionEntity session
            SET session.status =
                com.jeepclub.backend.authentication.core.domain.enums.SessionStatus.REVOKED
            WHERE session.userId = :userId
              AND session.status =
                com.jeepclub.backend.authentication.core.domain.enums.SessionStatus.ACTIVE
            """)
    void revokeActiveByUserId(@Param("userId") Long userId);
}
