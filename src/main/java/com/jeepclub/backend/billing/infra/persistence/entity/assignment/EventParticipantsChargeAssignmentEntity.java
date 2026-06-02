package com.jeepclub.backend.billing.infra.persistence.entity.assignment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(
        name = "billing_event_participants_charge_assignments",
        indexes = {
                @Index(
                        name = "idx_billing_event_participants_charge_assignments_event",
                        columnList = "event_id"
                )
        }
)
@DiscriminatorValue("EVENT_PARTICIPANTS")
@PrimaryKeyJoinColumn(name = "charge_assignment_id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventParticipantsChargeAssignmentEntity extends ChargeAssignmentEntity {

    @Column(
            name = "event_id",
            nullable = false
    )
    private Long eventId;

    public EventParticipantsChargeAssignmentEntity(
            Long id,
            Long chargeDefinitionId,
            Long eventId,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        super(
                id,
                chargeDefinitionId,
                active,
                createdAt,
                updatedAt
        );

        this.eventId = eventId;
    }
}