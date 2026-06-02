package com.jeepclub.backend.billing.infra.persistence.jpa.assignment;

import com.jeepclub.backend.billing.infra.persistence.entity.assignment.EventParticipantsChargeAssignmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventParticipantsChargeAssignmentJpaRepository
        extends JpaRepository<EventParticipantsChargeAssignmentEntity, Long> {

    boolean existsByChargeDefinitionIdAndEventId(
            Long chargeDefinitionId,
            Long eventId
    );
}