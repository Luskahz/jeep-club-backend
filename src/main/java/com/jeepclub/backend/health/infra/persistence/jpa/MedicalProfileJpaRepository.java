package com.jeepclub.backend.health.infra.persistence.jpa;

import com.jeepclub.backend.health.core.domain.MedicalProfileOwnerType;
import com.jeepclub.backend.health.infra.persistence.entity.MedicalProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedicalProfileJpaRepository extends JpaRepository<MedicalProfileEntity, Long> {

    Optional<MedicalProfileEntity> findByOwnerTypeAndOwnerId(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    );

    boolean existsByOwnerTypeAndOwnerId(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    );
}
