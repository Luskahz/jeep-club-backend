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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select m
            from MedicalProfileEntity m
            where m.ownerType = :ownerType
              and m.ownerId = :ownerId
            """)
    Optional<MedicalProfileEntity> findByOwnerForUpdate(
            @Param("ownerType") MedicalProfileOwnerType ownerType,
            @Param("ownerId") Long ownerId
    );

    boolean existsByOwnerTypeAndOwnerId(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    );


    @org.springframework.data.jpa.repository.Query(value="select id from medical_profiles where owner_type='DEPENDENT' and owner_id=:id limit 1 for update", nativeQuery=true)
    java.util.Optional<Long> lockDependentReference(@org.springframework.data.repository.query.Param("id") Long id);
}
