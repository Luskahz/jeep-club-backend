package com.jeepclub.backend.health.infra.integration.dependents;

import com.jeepclub.backend.dependents.api.module.DependentsQuery;
import com.jeepclub.backend.health.core.port.DependentOwnershipChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DependentsOwnershipAdapter implements DependentOwnershipChecker, com.jeepclub.backend.health.core.port.EmergencyDependentOwnershipChecker {

    private final DependentsQuery dependentsQuery;
    private final com.jeepclub.backend.dependents.api.module.DependentLifecycleLock lifecycle;
    @Override public boolean belongsToUserForEmergency(Long id,Long owner) {
        return lifecycle.byId(id).filter(d -> d.userId().equals(owner)).isPresent();
    }

    @Override
    public boolean belongsToUser(Long dependentId, Long userId) {
        return dependentsQuery.isActiveDependentOfUser(
                dependentId,
                userId
        );
    }
}
