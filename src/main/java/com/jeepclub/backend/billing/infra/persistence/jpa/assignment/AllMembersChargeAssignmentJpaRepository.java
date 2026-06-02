package com.jeepclub.backend.billing.infra.persistence.jpa.assignment;

import com.jeepclub.backend.billing.infra.persistence.entity.assignment.AllMembersChargeAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllMembersChargeAssignmentJpaRepository
        extends JpaRepository<AllMembersChargeAssignmentEntity, Long> {

    boolean existsByChargeDefinitionId(Long chargeDefinitionId);
}