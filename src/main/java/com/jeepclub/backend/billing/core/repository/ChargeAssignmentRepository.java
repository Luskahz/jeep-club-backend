package com.jeepclub.backend.billing.core.repository;

import com.jeepclub.backend.billing.core.domain.model.chargeAssignment.ChargeAssignment;
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

    boolean existsAllMembersAssignmentByChargeDefinitionId(Long chargeDefinitionId);

    boolean existsUserAssignmentByChargeDefinitionIdAndUserId(
            Long chargeDefinitionId,
            Long userId
    );

    boolean existsRoleAssignmentByChargeDefinitionIdAndRoleId(
            Long chargeDefinitionId,
            Long roleId
    );

    boolean existsEventParticipantsAssignmentByChargeDefinitionIdAndEventId(
            Long chargeDefinitionId,
            Long eventId
    );
}