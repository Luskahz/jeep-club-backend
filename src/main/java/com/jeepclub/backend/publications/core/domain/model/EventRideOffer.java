package com.jeepclub.backend.publications.core.domain.model;

import java.time.Instant;

public record EventRideOffer(Long id, Long eventId, Long guestRequestId, Long registrationId,
        Long userId, Long vehicleId, Status status, Instant respondedAt, Instant selectedAt) {
    public EventRideOffer {
        Publication.positive(eventId,"eventId"); Publication.positive(guestRequestId,"guestRequestId");
        Publication.positive(registrationId,"registrationId"); Publication.positive(userId,"userId");
        Publication.positive(vehicleId,"vehicleId"); java.util.Objects.requireNonNull(status);
        java.util.Objects.requireNonNull(respondedAt);
        if ((status == Status.SELECTED) != (selectedAt != null) || selectedAt != null && selectedAt.isBefore(respondedAt))
            throw new IllegalArgumentException("Inconsistent ride selection.");
    }
    public enum Status { ACCEPTED, DECLINED, SELECTED }
    public EventRideOffer select(Instant now) {
        if (status != Status.ACCEPTED) throw new IllegalStateException("Offer is not accepted.");
        return new EventRideOffer(id, eventId, guestRequestId, registrationId, userId, vehicleId, Status.SELECTED, respondedAt, now);
    }
    public EventRideOffer identified(Long value) {
        return new EventRideOffer(value, eventId, guestRequestId, registrationId, userId, vehicleId, status, respondedAt, selectedAt);
    }
}
