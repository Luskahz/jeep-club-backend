package com.jeepclub.backend.billing.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "billing_event_charge_contexts", uniqueConstraints = {
    @UniqueConstraint(name = "uk_event_charge_context", columnNames = {"event_id", "charge_definition_id"}),
    @UniqueConstraint(name = "uk_event_charge_context_cycle", columnNames = "cycle_id"),
    @UniqueConstraint(name = "uk_event_charge_context_assignment", columnNames = "assignment_id")
})
@Getter @Setter @NoArgsConstructor
public class EventChargeContextEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "event_id", nullable = false) private Long eventId;
    @Column(name = "charge_definition_id", nullable = false) private Long chargeDefinitionId;
    @Column(name = "assignment_id", nullable = false) private Long assignmentId;
    @Column(name = "cycle_id", nullable = false) private Long cycleId;
}
