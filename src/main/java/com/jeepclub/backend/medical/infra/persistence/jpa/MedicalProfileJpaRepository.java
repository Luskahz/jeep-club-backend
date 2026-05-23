package com.jeepclub.backend.medical.infra.persistence.jpa;

import com.jeepclub.backend.medical.core.domain.MedicalProfileOwnerType;
import com.jeepclub.backend.medical.infra.persistence.entity.MedicalProfileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// boa, o importante aqui é entender praque ser ve o extends JpaRepository<MedicalProfileEntity, Long> e como ele influencia nos nomes dos metodos
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