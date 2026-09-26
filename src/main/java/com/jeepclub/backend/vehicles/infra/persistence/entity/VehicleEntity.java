package com.jeepclub.backend.vehicles.infra.persistence.entity;

import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "vehicles_vehicle",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_vehicle_plate", columnNames = "plate"),
                @UniqueConstraint(name = "uk_vehicle_renavam", columnNames = "renavam")
        },
        indexes = {
                @Index(name = "idx_vehicle_owner_id_status", columnList = "owner_id, status")
        }
)
public class VehicleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String nickname;

    @Column(length = 500)
    private String photo;

    @Column(nullable = false, length = 7)
    private String plate;

    @Column(nullable = false, length = 11)
    private String renavam;

    @Column(nullable = false, length = 50)
    private String brand;

    @Column(nullable = false, length = 100)
    private String model;

    @Column(name = "manufacturing_year", nullable = false)
    private int manufacturingYear;

    @Column(name = "model_year", nullable = false)
    private int modelYear;

    @Column(length = 30)
    private String color;

    @Column(name = "seating_capacity", nullable = false)
    private int seatingCapacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false)
    private FuelType fuelType;

    @Column(name = "engine_displacement", nullable = false)
    private double engineDisplacement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    @Column(nullable = false)
    private Boolean towing;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
