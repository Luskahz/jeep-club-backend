package com.jeepclub.backend.health.core.port;

import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;

public interface MedicalProfileOwnerStatusChecker {

    MedicalProfileOwnerStatus getStatus(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    );
}
