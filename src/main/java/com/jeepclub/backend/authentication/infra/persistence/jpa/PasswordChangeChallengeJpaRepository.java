package com.jeepclub.backend.authentication.infra.persistence.jpa;

import com.jeepclub.backend.authentication.infra.persistence.entity.PasswordChangeChallengeEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PasswordChangeChallengeJpaRepository
        extends JpaRepository<PasswordChangeChallengeEntity, Long> {

    Optional<PasswordChangeChallengeEntity> findByTokenHash(
            String tokenHash
    );

    @Query("""
            SELECT challenge.userId
            FROM PasswordChangeChallengeEntity challenge
            WHERE challenge.tokenHash = :tokenHash
            """)
    Optional<Long> findUserIdByTokenHash(
            @Param("tokenHash") String tokenHash
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT challenge
            FROM PasswordChangeChallengeEntity challenge
            WHERE challenge.tokenHash = :tokenHash
            """)
    Optional<PasswordChangeChallengeEntity>
    findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE PasswordChangeChallengeEntity challenge
            SET challenge.used = true,
                challenge.usedAt = :now
            WHERE challenge.userId = :userId
              AND challenge.used = false
              AND challenge.expiresAt > :now
            """)
    int invalidateActiveByUserId(
            @Param("userId") Long userId,
            @Param("now") Instant now
    );
}
