package com.jeepclub.backend.medical.core.repository;

import com.jeepclub.backend.medical.core.domain.MedicalProfile;
import com.jeepclub.backend.medical.core.domain.MedicalProfileOwnerType;

import java.util.List;
import java.util.Optional;

public interface MedicalProfileRepository {

    Optional<MedicalProfile> findById(Long id);

    Optional<MedicalProfile> findByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    );

    List<MedicalProfile> findAll(int page, int size);

    MedicalProfile save(MedicalProfile medicalProfile);
}
