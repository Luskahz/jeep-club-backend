package com.jeepclub.backend.billing.core.application.query;
import com.jeepclub.backend.billing.api.module.ChargeDefinitionLock;
import com.jeepclub.backend.billing.core.repository.ChargeDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
@Service @RequiredArgsConstructor @Transactional(propagation=Propagation.MANDATORY)
class ChargeDefinitionLockService implements ChargeDefinitionLock {
    private final ChargeDefinitionRepository definitions;
    @Override public boolean isActive(Long id) {
        return id != null && id > 0 && definitions.findByIdForUpdate(id).map(d -> d.isActive()).orElse(false);
    }
}
