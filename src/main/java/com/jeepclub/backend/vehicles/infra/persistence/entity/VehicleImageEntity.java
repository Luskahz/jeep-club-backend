package com.jeepclub.backend.vehicles.infra.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
@Embeddable @Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class VehicleImageEntity {
    @Column(name="storage_key",nullable=false,length=500) private String storageKey;
    @Column(name="position",nullable=false) private int position;
    @Column(name="is_primary",nullable=false) private boolean primary;
}
