package com.jeepclub.backend.publications.api.http.dto;

import com.jeepclub.backend.publications.core.domain.model.EventGuestRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description="Stable guest ID identifies a participation; CPF is not unique. Origin never changes. APPROVED ADMIN without transport is STANDBY; only ALLOCATED consumes a seat.")
public record EventGuestResponseDTO(Long id,Long eventId,
        @Schema(description="Original requester/creator, never the current driver for ADMIN") Long requesterUserId,
        Long vehicleId,String cpf,EventGuestRequest.Status status,boolean administrative,Long reviewerId,
        Instant createdAt,Instant reviewedAt,String rejectionReason,String guestName,
        EventGuestRequest.Origin origin,EventGuestRequest.TransportStatus transportStatus,
        Long originRegistrationId,Long requestedVehicleId,Long allocatedRegistrationId,Long responsibleUserId,Long selectedOfferId) {
    public static EventGuestResponseDTO from(EventGuestRequest g) {
        return new EventGuestResponseDTO(g.id(),g.eventId(),g.requesterUserId(),g.vehicleId(),g.cpf(),g.status(),g.administrative(),g.reviewerId(),
            g.createdAt(),g.reviewedAt(),g.rejectionReason(),g.guestName(),g.origin(),g.transportStatus(),g.originRegistrationId(),g.requestedVehicleId(),
            g.allocatedRegistrationId(),g.responsibleUserId(),g.selectedOfferId());
    }
}
