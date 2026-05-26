package com.jeepclub.backend.billing.core.repository;

import com.jeepclub.backend.billing.core.domain.enums.ChargeAssignmentType;
import com.jeepclub.backend.billing.core.domain.model.ChargeAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ChargeAssignmentRepository {

    ChargeAssignment save(ChargeAssignment chargeAssignment);

    Optional<ChargeAssignment> findById(Long id);

    Page<ChargeAssignment> findByChargeDefinitionId(
            Long chargeDefinitionId,
            Pageable pageable
    );

    boolean existsByChargeDefinitionIdAndAssignmentTypeAndTargetId(
            Long chargeDefinitionId,
            ChargeAssignmentType assignmentType,
            Long targetId
    );
}