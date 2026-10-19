package com.jeepclub.backend.billing.infra.persistence.jpa.assignment;

import com.jeepclub.backend.billing.infra.persistence.entity.assignment.ChargeAssignmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChargeAssignmentJpaRepository extends JpaRepository<ChargeAssignmentEntity, Long> {

    Page<ChargeAssignmentEntity> findByChargeDefinitionId(
            Long chargeDefinitionId,
            Pageable pageable
    );

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<ChargeAssignmentEntity> findFirstByChargeDefinitionIdOrderById(Long definitionId);
}
