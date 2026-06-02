package com.jeepclub.backend.billing.infra.persistence.jpa.assignment;

import com.jeepclub.backend.billing.infra.persistence.entity.assignment.UserChargeAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserChargeAssignmentJpaRepository
        extends JpaRepository<UserChargeAssignmentEntity, Long> {

    boolean existsByChargeDefinitionIdAndUserId(
            Long chargeDefinitionId,
            Long userId
    );
}