package com.jeepclub.backend.billing.core.application.query;

import com.jeepclub.backend.billing.api.module.ChargeDefinitionQuery;
import com.jeepclub.backend.billing.core.repository.ChargeDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChargeDefinitionQueryService implements ChargeDefinitionQuery {

    private final ChargeDefinitionRepository repository;

    @Override
    @Transactional(readOnly = true)
    public boolean isActive(Long chargeDefinitionId) {
        if (chargeDefinitionId == null || chargeDefinitionId <= 0) {
            return false;
        }

        return repository.findById(chargeDefinitionId)
                .map(definition -> definition.isActive())
                .orElse(false);
    }
}
