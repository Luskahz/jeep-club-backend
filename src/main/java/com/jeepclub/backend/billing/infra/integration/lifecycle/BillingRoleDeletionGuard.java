package com.jeepclub.backend.billing.infra.integration.lifecycle;
import com.jeepclub.backend.iam.authorization.api.module.spi.RoleDeletionGuard;
import com.jeepclub.backend.billing.infra.persistence.jpa.assignment.RoleChargeAssignmentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class BillingRoleDeletionGuard implements RoleDeletionGuard {
    private final RoleChargeAssignmentJpaRepository repository;
    @Override public boolean hasRetainedReferences(Long id) {
        return repository.findFirstByRoleIdOrderById(id).isPresent();
    }
}
