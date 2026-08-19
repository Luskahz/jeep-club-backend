package com.jeepclub.backend.health.infra.integration.dependents;

import com.jeepclub.backend.dependents.api.module.DependentsQuery;
import com.jeepclub.backend.health.core.port.DependentOwnershipChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DependentsOwnershipAdapter implements DependentOwnershipChecker {

    private final DependentsQuery dependentsQuery;

    @Override
    public boolean belongsToUser(Long dependentId, Long userId) {
        return dependentsQuery.isActiveDependentOfUser(
                dependentId,
                userId
        );
    }
}