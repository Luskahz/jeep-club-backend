package com.jeepclub.backend.health.api.module.medicalprofile;

/**
 * Integration contract supplied by the module that owns dependents.
 */
public interface DependentOwnershipAccess {

    boolean belongsToUser(Long dependentId, Long userId);
}
