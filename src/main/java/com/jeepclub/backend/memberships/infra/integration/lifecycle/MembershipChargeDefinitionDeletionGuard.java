package com.jeepclub.backend.memberships.infra.integration.lifecycle;
import com.jeepclub.backend.billing.api.module.spi.ChargeDefinitionDeletionGuard;
import com.jeepclub.backend.memberships.infra.persistence.jpa.MembershipBillingConfigurationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class MembershipChargeDefinitionDeletionGuard implements ChargeDefinitionDeletionGuard {
    private final MembershipBillingConfigurationJpaRepository repository;
    @Override public boolean hasRetainedReferences(Long id) {
        return repository.findFirstByChargeDefinitionIdOrderById(id).isPresent();
    }
}
