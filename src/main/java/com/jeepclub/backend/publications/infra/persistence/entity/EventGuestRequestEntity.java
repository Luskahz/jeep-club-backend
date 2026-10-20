package com.jeepclub.backend.publications.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="event_guest_requests")
@Getter @Setter @NoArgsConstructor
public class EventGuestRequestEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Version private Long version;
    @Column(name="guest_name",length=150) private String guestName;
    @Column(name="event_id", nullable=false,updatable=false) private Long eventId;
    @Column(name="requester_user_id", nullable=false,updatable=false) private Long requesterUserId;
    @Column(name="origin_registration_id",updatable=false) private Long originRegistrationId;
    @Column(name="requested_vehicle_id",updatable=false) private Long requestedVehicleId;
    @Column(name="allocated_registration_id") private Long allocatedRegistrationId;
    @Column(name="responsible_user_id") private Long responsibleUserId;
    @Column(name="selected_offer_id") private Long selectedOfferId;
    @Column(name="vehicle_id", nullable=true) private Long vehicleId;
    @Column(name="approved_vehicle_id", nullable=true) private Long approvedVehicleId;
    @Column(name="cpf", nullable=false) private String cpf;
    @Column(name="status", nullable=false) private String status;
    @Column(name="administrative", nullable=false,updatable=false) private boolean administrative;
    @Column(name="reviewer_id", nullable=true) private Long reviewerId;
    @Column(name="created_at", nullable=false) private java.time.Instant createdAt;
    @Column(name="reviewed_at", nullable=true) private java.time.Instant reviewedAt;
    @Column(name="rejection_reason", nullable=true,length=1000) private String rejectionReason;

}
