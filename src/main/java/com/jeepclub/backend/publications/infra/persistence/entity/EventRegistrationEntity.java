package com.jeepclub.backend.publications.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="event_registrations", uniqueConstraints={@UniqueConstraint(name="uk_event_registration", columnNames={"event_id","user_id"})})
@Getter @Setter @NoArgsConstructor
public class EventRegistrationEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Version private Long version;
    @ElementCollection
    @org.hibernate.annotations.BatchSize(size=100)
    @CollectionTable(name="event_registration_unallocated_dependents", joinColumns=@JoinColumn(name="registration_id"),
        uniqueConstraints=@UniqueConstraint(name="uk_event_unallocated_dependent",columnNames={"registration_id","dependent_id"}))
    @Column(name="dependent_id",nullable=false)
    private java.util.List<Long> unallocatedDependentIds = new java.util.ArrayList<>();
    @Column(name="event_id", nullable=false) private Long eventId;
    @Column(name="user_id", nullable=false) private Long userId;
    @Column(name="status", nullable=false) private String status;
    @Column(name="created_at", nullable=false) private java.time.Instant createdAt;
    @Column(name="confirmed_at", nullable=true) private java.time.Instant confirmedAt;
    @Column(name="cancelled_at", nullable=true) private java.time.Instant cancelledAt;
    @ElementCollection
    @org.hibernate.annotations.BatchSize(size=100)
    @CollectionTable(name="event_registration_occupants", joinColumns=@JoinColumn(name="registration_id"),
        uniqueConstraints=@UniqueConstraint(name="uk_registration_occupant", columnNames={"registration_id","occupant_key"}))
    private java.util.List<EventOccupantEmbeddable> occupants = new java.util.ArrayList<>();
    @ElementCollection
    @org.hibernate.annotations.BatchSize(size=100)
    @CollectionTable(name="event_registration_vehicles", joinColumns=@JoinColumn(name="registration_id"),
        uniqueConstraints=@UniqueConstraint(name="uk_event_registration_vehicle", columnNames={"registration_id","vehicle_id"}))
    @Column(name="vehicle_id") private java.util.Set<Long> vehicleIds = new java.util.HashSet<>();
}
