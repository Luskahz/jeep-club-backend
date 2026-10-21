package com.jeepclub.backend.publications.core.domain.model;

import java.time.Instant;

public record EventRideOffer(Long id, Long eventId, Long guestRequestId, Long registrationId,
        Long userId, Long vehicleId, Status status, Instant respondedAt, Instant selectedAt, Instant withdrawnAt) {
    public EventRideOffer(Long id, Long eventId, Long guestRequestId, Long registrationId, Long userId, Long vehicleId,
            Status status, Instant respondedAt, Instant selectedAt) {
        this(id,eventId,guestRequestId,registrationId,userId,vehicleId,status,respondedAt,selectedAt,null);
    }
    public EventRideOffer {
        Publication.positive(eventId,"eventId"); Publication.positive(guestRequestId,"guestRequestId");
        Publication.positive(registrationId,"registrationId"); Publication.positive(userId,"userId");
        Publication.positive(vehicleId,"vehicleId"); java.util.Objects.requireNonNull(status);
        java.util.Objects.requireNonNull(respondedAt);
        if (status == Status.SELECTED && selectedAt == null
                || (status == Status.ACCEPTED || status == Status.DECLINED) && selectedAt != null
                || selectedAt != null && selectedAt.isBefore(respondedAt)
                || (status == Status.WITHDRAWN) != (withdrawnAt != null)
                || withdrawnAt != null && withdrawnAt.isBefore(selectedAt == null ? respondedAt : selectedAt))
            throw new IllegalArgumentException("Inconsistent ride selection.");
    }
    public enum Status { ACCEPTED, DECLINED, SELECTED, WITHDRAWN }
    public EventRideOffer withdraw(Instant now) {
        if (status == Status.WITHDRAWN) return this;
        if (status != Status.ACCEPTED && status != Status.SELECTED) throw new IllegalStateException("Offer has no active association.");
        return new EventRideOffer(id,eventId,guestRequestId,registrationId,userId,vehicleId,Status.WITHDRAWN,respondedAt,selectedAt,now);
    }
    public EventRideOffer decline() {
        if (status != Status.ACCEPTED) throw new IllegalStateException("Only accepted offers can be withdrawn.");
        return new EventRideOffer(id, eventId, guestRequestId, registrationId, userId, vehicleId,
            Status.DECLINED, respondedAt, null);
    }
    public EventRideOffer select(Instant now) {
        if (status != Status.ACCEPTED) throw new IllegalStateException("Offer is not accepted.");
        return new EventRideOffer(id, eventId, guestRequestId, registrationId, userId, vehicleId, Status.SELECTED, respondedAt, now);
    }
    public EventRideOffer identified(Long value) {
        return new EventRideOffer(value, eventId, guestRequestId, registrationId, userId, vehicleId, status, respondedAt, selectedAt, withdrawnAt);
    }
}
