package com.jeepclub.backend.billing.core.repository;

import com.jeepclub.backend.billing.core.domain.model.ChargeCycle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ChargeCycleRepository {
    java.util.List<com.jeepclub.backend.billing.core.domain.model.ChargeCycle> findByIds(java.util.Collection<Long> ids);

    java.util.List<ChargeCycle> findMembershipCycles(Long chargeDefinitionId);

    ChargeCycle save(ChargeCycle chargeCycle);

    Optional<ChargeCycle> findById(Long id);

    Optional<ChargeCycle> findByChargeDefinitionIdAndCode(
            Long chargeDefinitionId,
            String code
    );

    Page<ChargeCycle> findByChargeDefinitionId(
            Long chargeDefinitionId,
            Pageable pageable
    );

    boolean existsByChargeDefinitionIdAndCode(
            Long chargeDefinitionId,
            String code
    );
}
