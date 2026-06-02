package com.jeepclub.backend.health.core.ports;

public interface DependentOwnershipChecker {

    boolean belongsToUser(Long dependentId, Long userId);
}
