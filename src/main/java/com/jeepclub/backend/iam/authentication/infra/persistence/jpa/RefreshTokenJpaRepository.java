package com.jeepclub.backend.iam.authentication.infra.persistence.jpa;

import com.jeepclub.backend.iam.authentication.core.domain.enums.RefreshTokenStatus;
import com.jeepclub.backend.iam.authentication.infra.persistence.entity.RefreshTokenEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenJpaRepository
        extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(
            String tokenHash
    );

    @Query("""
            SELECT token.sessionId
            FROM RefreshTokenEntity token
            WHERE token.tokenHash = :tokenHash
            """)
    Optional<Long> findSessionIdByTokenHash(
            @Param("tokenHash") String tokenHash
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT token
            FROM RefreshTokenEntity token
            WHERE token.tokenHash = :tokenHash
            """)
    Optional<RefreshTokenEntity> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT token
            FROM RefreshTokenEntity token
            WHERE token.id = :id
            """)
    Optional<RefreshTokenEntity> findByIdForUpdate(
            @Param("id") Long id
    );

    Optional<RefreshTokenEntity> findBySessionIdAndStatus(
            Long sessionId,
            RefreshTokenStatus status
    );

    Optional<RefreshTokenEntity> findBySessionId(
            Long sessionId
    );

    List<RefreshTokenEntity>
    findBySessionIdInOrderByCreatedAtDesc(
            Collection<Long> sessionIds
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE RefreshTokenEntity token
            SET token.status =
                com.jeepclub.backend.authentication.core.domain.enums.RefreshTokenStatus.REVOKED,
                token.replacedByTokenId = null
            WHERE token.status =
                com.jeepclub.backend.authentication.core.domain.enums.RefreshTokenStatus.ACTIVE
              AND token.sessionId IN (
                  SELECT session.id
                  FROM SessionEntity session
                  WHERE session.userId = :userId
              )
            """)
    void revokeActiveByUserId(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE RefreshTokenEntity token
            SET token.status =
                com.jeepclub.backend.authentication.core.domain.enums.RefreshTokenStatus.REVOKED,
                token.replacedByTokenId = null
            WHERE token.status =
                com.jeepclub.backend.authentication.core.domain.enums.RefreshTokenStatus.ACTIVE
              AND token.sessionId = :sessionId
            """)
    void revokeActiveBySessionId(@Param("sessionId") Long sessionId);
}
