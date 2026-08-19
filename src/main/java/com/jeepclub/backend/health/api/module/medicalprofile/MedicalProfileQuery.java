package com.jeepclub.backend.health.api.module.medicalprofile;
public interface MedicalProfileQuery {

    boolean existsByOwner(
            MedicalProfileOwner ownerType,
            Long ownerId
    );
}