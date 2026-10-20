package com.jeepclub.backend.publications.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name="event_guest_transitions",indexes=@Index(name="idx_event_guest_transition",columnList="event_id,guest_request_id,id"))
@org.hibernate.annotations.Immutable
@Getter @Setter @NoArgsConstructor
public class EventGuestTransitionEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="event_id",nullable=false,updatable=false) private Long eventId;
    @Column(name="guest_request_id",nullable=false,updatable=false) private Long guestRequestId;
    @Column(nullable=false,length=80,updatable=false) private String action;
    @Column(name="actor_user_id",updatable=false) private Long actorUserId;
    @Column(nullable=false,length=1000,updatable=false) private String reason;
    @Column(name="occurred_at",nullable=false,updatable=false) private Instant occurredAt;
    @Column(name="previous_status",length=30,updatable=false) private String previousStatus;
    @Column(name="next_status",nullable=false,length=30,updatable=false) private String nextStatus;
    @Column(name="previous_vehicle_id",updatable=false) private Long previousVehicleId;
    @Column(name="next_vehicle_id",updatable=false) private Long nextVehicleId;
    @Column(name="previous_registration_id",updatable=false) private Long previousRegistrationId;
    @Column(name="next_registration_id",updatable=false) private Long nextRegistrationId;
    @Column(name="offer_id",updatable=false) private Long offerId;
}
