package com.jeepclub.backend.publications.core.domain.model;

import java.time.Instant;
import java.util.Objects;

/** Participation is independent from transport. Origin and its registration never change. */
public record EventGuestRequest(Long id, Long eventId, Long requesterUserId, Long vehicleId, String cpf,
        Status status, boolean administrative, Long reviewerId, Instant createdAt, Instant reviewedAt,
        String rejectionReason, String guestName, Long originRegistrationId, Long requestedVehicleId,
        Long allocatedRegistrationId, Long responsibleUserId, Long selectedOfferId) {
    public enum Status { PENDING, APPROVED, REJECTED, CANCELLED }
    public enum Origin { MEMBER, ADMIN }
    public enum TransportStatus { NONE, STANDBY, ALLOCATED }
    public Origin origin() { return administrative ? Origin.ADMIN : Origin.MEMBER; }
    public TransportStatus transportStatus() {
        return status != Status.APPROVED ? TransportStatus.NONE
            : vehicleId == null ? TransportStatus.STANDBY : TransportStatus.ALLOCATED;
    }
    public EventGuestRequest {
        Publication.positive(eventId, "eventId");
        Publication.positive(requesterUserId, "requesterUserId");
        cpf = Objects.requireNonNull(cpf, "cpf").replaceAll("\\D", "");
        if (cpf.length() != 11) throw new IllegalArgumentException("CPF requires eleven digits.");
        Objects.requireNonNull(status); Objects.requireNonNull(createdAt);
        if (guestName != null) {
            guestName = guestName.trim();
            if (guestName.isEmpty() || guestName.length() > 150) throw new IllegalArgumentException("Guest name requires 1 to 150 characters.");
        }
        if (administrative) {
            if (originRegistrationId != null || requestedVehicleId != null || status == Status.PENDING)
                throw new IllegalArgumentException("Administrative origin has no member registration or pending review.");
        } else {
            Publication.positive(originRegistrationId, "originRegistrationId");
            Publication.positive(requestedVehicleId, "requestedVehicleId");
            if (status == Status.APPROVED && vehicleId == null) throw new IllegalArgumentException("Approved personal guest requires transport.");
        }
        if (vehicleId != null) {
            Publication.positive(vehicleId, "vehicleId");
            Publication.positive(allocatedRegistrationId, "allocatedRegistrationId");
            Publication.positive(responsibleUserId, "responsibleUserId");
            if (status != Status.APPROVED) throw new IllegalArgumentException("Only active approved participation can occupy a seat.");
        } else if (allocatedRegistrationId != null || responsibleUserId != null || selectedOfferId != null) {
            throw new IllegalArgumentException("Unallocated guest cannot retain a transport association.");
        }
        if (selectedOfferId != null) Publication.positive(selectedOfferId, "selectedOfferId");
    }
    public static EventGuestRequest requested(Long event, Long user, Long registration, Long vehicle, String cpf, String name, Instant now) {
        return new EventGuestRequest(null,event,user,null,cpf,Status.PENDING,false,null,now,null,null,name,registration,vehicle,null,null,null);
    }
    public static EventGuestRequest standby(Long event, Long actor, String cpf, String name, Instant now) {
        return new EventGuestRequest(null,event,actor,null,cpf,Status.APPROVED,true,null,now,null,null,name,null,null,null,null,null);
    }
    public EventGuestRequest review(boolean approve, Long registration, Long responsible, Long actor, String reason, Instant now) {
        if (administrative || status != Status.PENDING) throw new IllegalStateException("Guest already reviewed or administrative.");
        Publication.positive(actor, "reviewerId");
        if (!approve && (reason == null || reason.isBlank())) throw new IllegalArgumentException("Rejection reason required.");
        return new EventGuestRequest(id,eventId,requesterUserId,approve ? requestedVehicleId : null,cpf,
            approve ? Status.APPROVED : Status.REJECTED,false,actor,createdAt,now,approve ? null : reason.trim(),
            guestName,originRegistrationId,requestedVehicleId,approve ? registration : null,approve ? responsible : null,null);
    }
    public EventGuestRequest allocate(Long vehicle, Long registration, Long responsible, Long offer) {
        if (!administrative || status != Status.APPROVED) throw new IllegalStateException("Only active administrative guests can change transport.");
        return copy(Status.APPROVED,vehicle,registration,responsible,offer);
    }
    public EventGuestRequest unassign() {
        if (!administrative || status != Status.APPROVED) throw new IllegalStateException("Only active administrative guests return to standby.");
        return copy(status,null,null,null,null);
    }
    public EventGuestRequest cancel() {
        if (status == Status.CANCELLED) return this;
        if (status != Status.PENDING && status != Status.APPROVED) throw new IllegalStateException("Rejected participation cannot be cancelled.");
        return copy(Status.CANCELLED,null,null,null,null);
    }
    public EventGuestRequest reactivate() {
        if (!administrative || status != Status.CANCELLED) throw new IllegalStateException("Only cancelled administrative guests can reactivate.");
        return copy(Status.APPROVED,null,null,null,null);
    }
    private EventGuestRequest copy(Status next,Long vehicle,Long registration,Long responsible,Long offer) {
        return new EventGuestRequest(id,eventId,requesterUserId,vehicle,cpf,next,administrative,reviewerId,createdAt,reviewedAt,
            rejectionReason,guestName,originRegistrationId,requestedVehicleId,registration,responsible,offer);
    }
    public EventGuestRequest identified(Long value) {
        return new EventGuestRequest(value,eventId,requesterUserId,vehicleId,cpf,status,administrative,reviewerId,createdAt,reviewedAt,
            rejectionReason,guestName,originRegistrationId,requestedVehicleId,allocatedRegistrationId,responsibleUserId,selectedOfferId);
    }
}
