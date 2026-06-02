package com.jeepclub.backend.billing.infra.persistence.jpa;

import com.jeepclub.backend.billing.infra.persistence.entity.ChargeCycleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChargeCycleJpaRepository extends JpaRepository<ChargeCycleEntity, Long> {

    Optional<ChargeCycleEntity> findByChargeDefinitionIdAndCode(
            Long chargeDefinitionId,
            String code
    );

    Page<ChargeCycleEntity> findByChargeDefinitionId(
            Long chargeDefinitionId,
            Pageable pageable
    );

    boolean existsByChargeDefinitionIdAndCode(
            Long chargeDefinitionId,
            String code
    );
}