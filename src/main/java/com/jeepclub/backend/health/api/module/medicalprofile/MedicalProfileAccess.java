package com.jeepclub.backend.health.api.module.medicalprofile;

import java.util.Optional;

/**
 * Public module boundary for integrations that need to read or maintain a
 * medical profile without depending on Health internals.
 */
public interface MedicalProfileAccess {

    Optional<MedicalProfileView> findByOwner(
            MedicalProfileOwner ownerType,
            Long ownerId
    );

    MedicalProfileView upsertByOwner(
            MedicalProfileOwner ownerType,
            Long ownerId,
            MedicalProfileCommand command
    );
}
