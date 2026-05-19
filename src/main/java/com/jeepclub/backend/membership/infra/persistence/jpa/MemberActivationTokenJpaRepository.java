package com.jeepclub.backend.membership.infra.persistence.jpa;

import com.jeepclub.backend.membership.infra.persistence.entity.MemberActivationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface MemberActivationTokenJpaRepository extends JpaRepository<MemberActivationTokenEntity, Long> {

    Optional<MemberActivationTokenEntity> findByTokenHash(String tokenHash);

    Optional<MemberActivationTokenEntity> findTopByApplicationIdOrderByCreatedAtDesc(Long applicationId);

    @Modifying
    @Query("UPDATE MemberActivationTokenEntity t SET t.usedAt = :now WHERE t.applicationId = :applicationId AND t.usedAt IS NULL")
    void invalidateAllByApplicationId(@Param("applicationId") Long applicationId, @Param("now") Instant now);
}