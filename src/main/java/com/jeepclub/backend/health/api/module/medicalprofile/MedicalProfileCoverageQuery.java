package com.jeepclub.backend.health.api.module.medicalprofile;
/** Existence only: never returns clinical data. Input is limited to 500 IDs. */
public interface MedicalProfileCoverageQuery {
    java.util.Set<Long> findCovered(MedicalProfileOwner type, java.util.Collection<Long> ids);
}
