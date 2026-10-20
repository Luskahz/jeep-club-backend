package com.jeepclub.backend.dependents.core.application.query;
import com.jeepclub.backend.dependents.api.module.DependentLifecycleLock;
import com.jeepclub.backend.dependents.core.repository.DependentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.Optional;
@Service @RequiredArgsConstructor @Transactional(propagation=Propagation.MANDATORY)
class DependentLifecycleLockService implements DependentLifecycleLock {
    private final DependentRepository dependents;
    @Override public Optional<Eligibility> byId(Long id) {
        return dependents.findByIdForUpdate(id).map(d -> new Eligibility(d.getId(),d.getUserId(),d.isActive()));
    }
}
