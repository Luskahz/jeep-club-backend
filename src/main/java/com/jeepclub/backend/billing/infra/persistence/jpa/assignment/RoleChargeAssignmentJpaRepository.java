package com.jeepclub.backend.billing.infra.persistence.jpa.assignment;

import com.jeepclub.backend.billing.infra.persistence.entity.assignment.RoleChargeAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleChargeAssignmentJpaRepository
        extends JpaRepository<RoleChargeAssignmentEntity, Long> {

    boolean existsByChargeDefinitionIdAndRoleId(
            Long chargeDefinitionId,
            Long roleId
    );

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    java.util.Optional<RoleChargeAssignmentEntity> findFirstByRoleIdOrderById(Long id);
}
