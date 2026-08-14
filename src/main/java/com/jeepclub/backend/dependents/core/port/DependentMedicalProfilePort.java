package com.jeepclub.backend.dependents.core.port;

public interface DependentMedicalProfilePort {

    DependentMedicalProfileData findByDependentId(Long dependentId);

    void upsert(Long dependentId, DependentMedicalProfileData medicalProfile);
}
