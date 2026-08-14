package com.jeepclub.backend.dependents.infra.integration.health;

import com.jeepclub.backend.dependents.infra.persistence.jpa.DependentJpaRepository;
import com.jeepclub.backend.health.api.module.medicalprofile.DependentOwnershipAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HealthDependentOwnershipAccessAdapter implements DependentOwnershipAccess {

    private final DependentJpaRepository dependentJpaRepository;

    @Override
    public boolean belongsToUser(Long dependentId, Long userId) {
        if (dependentId == null || userId == null) {
            return false;
        }

        return dependentJpaRepository.existsByIdAndSocioId(dependentId, userId);
    }
}
