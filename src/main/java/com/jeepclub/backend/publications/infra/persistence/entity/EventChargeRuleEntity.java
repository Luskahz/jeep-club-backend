package com.jeepclub.backend.publications.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name="event_charge_rules", uniqueConstraints={@UniqueConstraint(name="uk_event_charge_rule", columnNames={"event_id","charge_definition_id"})})
@Getter @Setter @NoArgsConstructor
public class EventChargeRuleEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Version private Long version;
    @Column(name="event_id", nullable=false) private Long eventId;
    @Column(name="charge_definition_id", nullable=false) private Long chargeDefinitionId;
    @Column(name="required_for_participation", nullable=false) private boolean requiredForParticipation;
    @Column(name="participation_cutoff", nullable=false) private java.time.Instant participationCutoff;

}
