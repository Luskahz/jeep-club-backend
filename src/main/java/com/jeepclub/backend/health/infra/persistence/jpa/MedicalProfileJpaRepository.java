package com.jeepclub.backend.health.infra.persistence.jpa;

import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.infra.persistence.entity.MedicalProfileEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MedicalProfileJpaRepository extends JpaRepository<MedicalProfileEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select m
            from MedicalProfileEntity m
            where m.id = :id
            """)
    Optional<MedicalProfileEntity> findByIdForUpdate(
            @Param("id") Long id
    );

    Optional<MedicalProfileEntity> findByOwnerTypeAndOwnerId(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    );

    boolean existsByOwnerTypeAndOwnerId(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    );
}
