package com.jeepclub.backend.medical.core.repository;

import com.jeepclub.backend.medical.core.domain.MedicalProfile;
import com.jeepclub.backend.medical.core.domain.MedicalProfileOwnerType;

import java.util.Optional;

public interface MedicalProfileRepository {

    Optional<MedicalProfile> findByOwner(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    );

    MedicalProfile save(MedicalProfile medicalProfile);
}