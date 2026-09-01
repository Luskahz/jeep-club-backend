package com.jeepclub.backend.health.core.repository;

import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import com.jeepclub.backend.health.core.domain.model.MedicalProfile;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MedicalProfileRepository {

    MedicalProfile save(MedicalProfile medicalProfile);

    Optional<MedicalProfile> findById(Long id);

    Optional<MedicalProfile> findByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    );

    boolean existsByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    );

    List<MedicalProfile> findAll(
            int page,
            int size
    );

    void delete(
            MedicalProfile medicalProfile,
            Long deletedByUserId,
            Instant deletedAt
    );
}
