package com.jeepclub.backend.billing.infra.persistence.jpa;

import com.jeepclub.backend.billing.infra.persistence.entity.ChargeCycleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChargeCycleJpaRepository extends JpaRepository<ChargeCycleEntity, Long> {
    @org.springframework.data.jpa.repository.Query("select c from ChargeCycleEntity c where c.chargeDefinitionId = :definitionId and not exists (select e.id from EventChargeContextEntity e where e.cycleId = c.id)")
    java.util.List<ChargeCycleEntity> findMembershipCycles(Long definitionId);

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
