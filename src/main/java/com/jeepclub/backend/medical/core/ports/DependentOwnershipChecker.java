package com.jeepclub.backend.medical.core.ports;

public interface DependentOwnershipChecker {

    boolean belongsToUser(Long dependentId, Long userId);
}
