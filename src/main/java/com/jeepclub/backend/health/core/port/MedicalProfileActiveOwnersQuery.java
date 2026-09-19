package com.jeepclub.backend.health.core.port;

import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;

import java.util.Collection;
import java.util.Set;

public interface MedicalProfileActiveOwnersQuery {

    Set<Long> findActiveOwnerIds(
            MedicalProfileOwnerType ownerType,
            Collection<Long> ownerIds
    );
}
