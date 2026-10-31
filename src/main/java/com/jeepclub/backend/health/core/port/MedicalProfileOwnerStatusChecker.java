package com.jeepclub.backend.health.core.port;

import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;

public interface MedicalProfileOwnerStatusChecker {

    MedicalProfileOwnerStatus getStatus(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    );

    /** Called inside writes, before taking the profile lock. */
    MedicalProfileOwnerStatus getStatusForUpdate(MedicalProfileOwnerType type,Long id);

    /** Identity IDs ascending, then dependent, before the profile/history write. */
    void lockForDeletion(MedicalProfileOwnerType type, Long ownerId, Long actorId);
}
