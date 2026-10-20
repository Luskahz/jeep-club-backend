package com.jeepclub.backend.publications.infra.integration.lifecycle;
import com.jeepclub.backend.billing.api.module.spi.ChargeDefinitionDeletionGuard;
import com.jeepclub.backend.publications.infra.persistence.jpa.EventChargeRuleJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class PublicationChargeDefinitionDeletionGuard implements ChargeDefinitionDeletionGuard {
    private final EventChargeRuleJpaRepository repository;
    @Override public boolean hasRetainedReferences(Long id) {
        return repository.findFirstByChargeDefinitionIdOrderById(id).isPresent();
    }
}
