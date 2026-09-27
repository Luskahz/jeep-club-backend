package com.jeepclub.backend.publications.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="event_ride_offers", uniqueConstraints={@UniqueConstraint(name="uk_event_ride_response", columnNames={"guest_request_id","vehicle_id"}), @UniqueConstraint(name="uk_event_ride_selected", columnNames={"selected_guest_id"})})
@Getter @Setter @NoArgsConstructor
public class EventRideOfferEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Version private Long version;
    @Column(name="event_id", nullable=false) private Long eventId;
    @Column(name="guest_request_id", nullable=false) private Long guestRequestId;
    @Column(name="selected_guest_id", nullable=true) private Long selectedGuestId;
    @Column(name="registration_id", nullable=false) private Long registrationId;
    @Column(name="user_id", nullable=false) private Long userId;
    @Column(name="vehicle_id", nullable=false) private Long vehicleId;
    @Column(name="status", nullable=false) private String status;
    @Column(name="responded_at", nullable=false) private java.time.Instant respondedAt;
    @Column(name="selected_at", nullable=true) private java.time.Instant selectedAt;

}
