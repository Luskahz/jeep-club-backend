package com.jeepclub.backend.dependents.infra.health;

import com.jeepclub.backend.dependents.infra.persistence.jpa.DependentJpaRepository;
import com.jeepclub.backend.health.core.ports.DependentOwnershipChecker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DependentOwnershipCheckerAdapter implements DependentOwnershipChecker {

    private final DependentJpaRepository dependentJpaRepository;

    @Override
    public boolean belongsToUser(Long dependentId, Long userId) {
        if (dependentId == null || userId == null) {
            return false;
        }

        return dependentJpaRepository.existsByIdAndSocioId(dependentId, userId);
    }
}
