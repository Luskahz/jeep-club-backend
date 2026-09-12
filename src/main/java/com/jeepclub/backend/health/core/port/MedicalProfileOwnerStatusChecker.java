package com.jeepclub.backend.health.core.port;

import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;

import java.util.Collection;
import java.util.Set;

public interface MedicalProfileOwnerStatusChecker {

    MedicalProfileOwnerStatus getStatus(
            MedicalProfileOwnerType ownerType,
            Long ownerId
    );

    Set<Long> findActiveOwnerIds(
            MedicalProfileOwnerType ownerType,
            Collection<Long> ownerIds
    );
}
