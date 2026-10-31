package com.jeepclub.backend.health.core.port;

import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;

import java.util.Collection;
import java.util.Map;

/** Current registration names only; callers must establish profile eligibility first. */
public interface MedicalProfileOwnerPresentationQuery {
    record Owner(MedicalProfileOwnerType type, Long id) {}
    record Names(String ownerName, String responsibleName) {
        @Override public String toString() { return "Names[REDACTED]"; }
    }

    Map<Owner, Names> resolve(Collection<Owner> owners);
}
