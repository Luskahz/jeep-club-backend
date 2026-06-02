package com.jeepclub.backend.billing.core.domain.model.assignment;

import com.jeepclub.backend.billing.core.domain.enums.assignment.ChargeAudienceType;
import lombok.Getter;

import java.time.Instant;

@Getter
public final class EventParticipantsChargeAssignment extends ChargeAssignment {

    private final Long eventId;

    private EventParticipantsChargeAssignment(
            Long id,
            Long chargeDefinitionId,
            Long eventId,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        super(id, chargeDefinitionId, active, createdAt, updatedAt);
        this.eventId = validateId(eventId, "eventId");
    }

    public static EventParticipantsChargeAssignment create(
            Long chargeDefinitionId,
            Long eventId,
            Instant now
    ) {
        return new EventParticipantsChargeAssignment(
                null,
                chargeDefinitionId,
                eventId,
                true,
                now,
                null
        );
    }

    public static EventParticipantsChargeAssignment reconstitute(
            Long id,
            Long chargeDefinitionId,
            Long eventId,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new EventParticipantsChargeAssignment(
                id,
                chargeDefinitionId,
                eventId,
                active,
                createdAt,
                updatedAt
        );
    }

    @Override
    public ChargeAudienceType audienceType() {
        return ChargeAudienceType.EVENT_PARTICIPANTS;
    }
}