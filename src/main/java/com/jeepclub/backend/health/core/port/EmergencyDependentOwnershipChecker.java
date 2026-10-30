package com.jeepclub.backend.health.core.port;

/** Current locked ownership for an already-confirmed Event participant. */
public interface EmergencyDependentOwnershipChecker {
    boolean belongsToUserForEmergency(Long dependentId,Long responsibleUserId);
}
