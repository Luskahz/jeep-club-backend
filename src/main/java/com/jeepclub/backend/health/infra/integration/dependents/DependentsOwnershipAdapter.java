package com.jeepclub.backend.health.infra.integration.dependents;

import com.jeepclub.backend.health.api.module.medicalprofile.DependentOwnershipAccess;
import com.jeepclub.backend.health.core.port.DependentOwnershipChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DependentsOwnershipAdapter implements DependentOwnershipChecker {

    private final DependentOwnershipAccess dependentOwnershipAccess;

    @Override
    public boolean belongsToUser(Long dependentId, Long userId) {
        return dependentOwnershipAccess.belongsToUser(dependentId, userId);
    }
}
