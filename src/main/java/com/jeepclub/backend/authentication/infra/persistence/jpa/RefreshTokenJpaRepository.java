package com.jeepclub.backend.authentication.infra.persistence.jpa;

import com.jeepclub.backend.authentication.core.domain.enums.RefreshTokenStatus;
import com.jeepclub.backend.authentication.infra.persistence.entity.RefreshTokenEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenJpaRepository
        extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

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

    Optional<RefreshTokenEntity> findBySessionId(Long sessionId);

    List<RefreshTokenEntity> findBySessionIdInOrderByCreatedAtDesc(
            Collection<Long> sessionIds
    );


}