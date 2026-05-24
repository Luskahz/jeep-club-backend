package com.jeepclub.backend.medical.core.application.ports;

public interface DependentOwnershipChecker {

    boolean belongsToUser(Long dependentId, Long userId);
}
