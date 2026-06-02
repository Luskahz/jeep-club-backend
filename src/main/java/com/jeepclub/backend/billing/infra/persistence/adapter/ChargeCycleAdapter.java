package com.jeepclub.backend.billing.infra.persistence.adapter;

import com.jeepclub.backend.billing.core.domain.model.ChargeCycle;
import com.jeepclub.backend.billing.core.repository.ChargeCycleRepository;
import com.jeepclub.backend.billing.infra.persistence.entity.ChargeCycleEntity;
import com.jeepclub.backend.billing.infra.persistence.jpa.ChargeCycleJpaRepository;
import com.jeepclub.backend.billing.infra.persistence.mapper.ChargeCycleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChargeCycleAdapter implements ChargeCycleRepository {

    private final ChargeCycleJpaRepository jpa;
    private final ChargeCycleMapper mapper;

    @Override
    public ChargeCycle save(ChargeCycle chargeCycle) {
        ChargeCycleEntity entity = mapper.toEntity(chargeCycle);
        ChargeCycleEntity savedEntity = jpa.save(entity);

        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<ChargeCycle> findById(Long id) {
        return jpa.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<ChargeCycle> findByChargeDefinitionIdAndCode(
            Long chargeDefinitionId,
            String code
    ) {
        return jpa.findByChargeDefinitionIdAndCode(chargeDefinitionId, code)
                .map(mapper::toDomain);
    }

    @Override
    public Page<ChargeCycle> findByChargeDefinitionId(
            Long chargeDefinitionId,
            Pageable pageable
    ) {
        return jpa.findByChargeDefinitionId(chargeDefinitionId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByChargeDefinitionIdAndCode(
            Long chargeDefinitionId,
            String code
    ) {
        return jpa.existsByChargeDefinitionIdAndCode(
                chargeDefinitionId,
                code
        );
    }
}