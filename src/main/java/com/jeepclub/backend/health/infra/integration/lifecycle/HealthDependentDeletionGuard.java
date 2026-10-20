package com.jeepclub.backend.health.infra.integration.lifecycle;
import com.jeepclub.backend.dependents.api.module.spi.DependentDeletionGuard;
import com.jeepclub.backend.health.infra.persistence.jpa.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class HealthDependentDeletionGuard implements DependentDeletionGuard {
    private final MedicalProfileJpaRepository current;
    private final MedicalProfileHistoryJpaRepository history;
    @Override public boolean hasRetainedReferences(Long id) {
        return current.lockDependentReference(id).isPresent() || history.lockDependentReference(id).isPresent();
    }
}
