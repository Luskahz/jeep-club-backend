package com.jeepclub.backend.publications.infra.integration.lifecycle;
import com.jeepclub.backend.dependents.api.module.spi.DependentDeletionGuard;
import com.jeepclub.backend.publications.infra.persistence.jpa.EventRegistrationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
@Component @RequiredArgsConstructor
public class PublicationDependentDeletionGuard implements DependentDeletionGuard {
    private final EventRegistrationJpaRepository references;
    @Override public boolean hasRetainedReferences(Long id) { return references.lockDependentAllocation(id).isPresent() || references.lockUnallocatedDependent(id).isPresent(); }
}
