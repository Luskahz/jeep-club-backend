package com.jeepclub.backend.health.core.repository;

import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;

public interface MedicalProfileRepository {

    MedicalProfile save(MedicalProfile medicalProfile);

    Optional<MedicalProfile> findById(Long id);

    Optional<MedicalProfile> findByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    );

    Optional<MedicalProfile> findByOwnerForUpdate(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    );

    boolean existsByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    );

    Page<MedicalProfile> findAll(Pageable pageable);

    void delete(
            MedicalProfile medicalProfile,
            Long deletedByUserId,
            Instant deletedAt
    );
}
