package com.jeepclub.backend.billing.core.repository;

import com.jeepclub.backend.billing.core.domain.enums.ChargeAssignmentType;
import com.jeepclub.backend.billing.core.domain.model.ChargeAssignment;

import java.util.List;
import java.util.Optional;

public interface ChargeAssignmentRepository {

    ChargeAssignment save(ChargeAssignment chargeAssignment);

    Optional<ChargeAssignment> findById(Long id);

    List<ChargeAssignment> findByChargeDefinitionId(Long chargeDefinitionId);

    boolean existsByChargeDefinitionIdAndAssignmentTypeAndTargetId(
            Long chargeDefinitionId,
            ChargeAssignmentType assignmentType,
            Long targetId
    );
}