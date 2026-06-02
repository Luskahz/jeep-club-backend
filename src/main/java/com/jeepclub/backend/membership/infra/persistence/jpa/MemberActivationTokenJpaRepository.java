package com.jeepclub.backend.membership.infra.persistence.jpa;

import com.jeepclub.backend.membership.infra.persistence.entity.MemberActivationTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberActivationTokenJpaRepository
        extends JpaRepository<MemberActivationTokenEntity, Long> {

    Optional<MemberActivationTokenEntity> findByTokenHash(String tokenHash);

    // O metodo derivado do JPA não suporta LIMIT com ORDER BY de forma portável,
    // portanto o @Query é necessário e mantido aqui intencionalmente.
    @Query("SELECT t FROM MemberActivationTokenEntity t WHERE t.applicationId = :applicationId ORDER BY t.createdAt DESC LIMIT 1")
    Optional<MemberActivationTokenEntity> findLatestByApplicationId(@Param("applicationId") Long applicationId);

    @Modifying
    @Query("UPDATE MemberActivationTokenEntity t SET t.usedAt = CURRENT_TIMESTAMP WHERE t.applicationId = :applicationId AND t.usedAt IS NULL")
    void invalidateAllByApplicationId(@Param("applicationId") Long applicationId);
}