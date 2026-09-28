package com.jeepclub.backend.health.core.repository;
import com.jeepclub.backend.health.api.module.medicalprofile.MedicalProfileOwner;
public interface MedicalProfileCoverageRepository {
    java.util.Set<Long> findCovered(MedicalProfileOwner type,java.util.Collection<Long> ids);
}
