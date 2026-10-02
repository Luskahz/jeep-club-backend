package com.jeepclub.backend.publications.infra.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
@Embeddable @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class EventOccupantEmbeddable {
    @Column(name="vehicle_id", nullable=false) private Long vehicleId;
    @Column(name="occupant_key", nullable=false) private String occupantKey;
    private Long dependentId;
}
