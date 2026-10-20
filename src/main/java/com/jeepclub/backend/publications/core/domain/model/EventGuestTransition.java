package com.jeepclub.backend.publications.core.domain.model;

import java.time.Instant;
import java.util.Objects;

/** Independent immutable audit. No guest PII or destructive parent relationships. */
public record EventGuestTransition(Long id, Long eventId, Long guestRequestId, String action, Long actorUserId,
        String reason, Instant occurredAt, String previousStatus, String nextStatus, Long previousVehicleId,
        Long nextVehicleId, Long previousRegistrationId, Long nextRegistrationId, Long offerId) {
    public EventGuestTransition {
        Publication.positive(eventId,"eventId"); Publication.positive(guestRequestId,"guestRequestId");
        Objects.requireNonNull(action); Objects.requireNonNull(occurredAt); Objects.requireNonNull(nextStatus);
        if (!action.equals("LEGACY_MIGRATION")) Publication.positive(actorUserId,"actorUserId");
        if (reason == null || reason.isBlank() || reason.length() > 1000) throw new IllegalArgumentException("Reason requires 1 to 1000 characters.");
    }
}
