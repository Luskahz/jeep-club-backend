package com.jeepclub.backend.health.core.port;

public interface DependentOwnershipChecker {

    boolean belongsToUser(Long dependentId, Long userId);
}
