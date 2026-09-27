package com.jeepclub.backend.publications.core.domain.model;

import java.time.Instant;
import java.util.Objects;

public record EventGuestRequest(Long id, Long eventId, Long requesterUserId, Long vehicleId, String cpf,
        Status status, boolean administrative, Long reviewerId, Instant createdAt, Instant reviewedAt, String rejectionReason) {
    public enum Status { PENDING, APPROVED, REJECTED }
    public EventGuestRequest {
        Publication.positive(eventId, "eventId"); Publication.positive(requesterUserId, "requesterUserId");
        cpf = Objects.requireNonNull(cpf, "cpf").replaceAll("\\D", "");
        if (cpf.length() != 11) throw new IllegalArgumentException("CPF requires eleven digits.");
        Objects.requireNonNull(status); Objects.requireNonNull(createdAt);
    }
    public EventGuestRequest review(boolean approve, Long vehicle, Long actor, String reason, Instant now) {
        if (status != Status.PENDING) throw new IllegalStateException("Guest already reviewed.");
        Publication.positive(actor, "reviewerId");
        if (approve) Publication.positive(vehicle, "vehicleId");
        else if (reason == null || reason.isBlank()) throw new IllegalArgumentException("Rejection reason required.");
        return new EventGuestRequest(id, eventId, requesterUserId, vehicle, cpf, approve ? Status.APPROVED : Status.REJECTED,
            administrative, actor, createdAt, now, approve ? null : reason.trim());
    }
    public EventGuestRequest identified(Long value) {
        return new EventGuestRequest(value, eventId, requesterUserId, vehicleId, cpf, status, administrative, reviewerId, createdAt, reviewedAt, rejectionReason);
    }
}
