package com.jeepclub.backend.vehicles.infra.persistence.entity;

import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "vehicles_vehicle_history",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_vehicle_history_vehicle_id",
                        columnNames = "vehicle_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_vehicle_history_owner_id",
                        columnList = "owner_id"
                ),
                @Index(
                        name = "idx_vehicle_history_plate",
                        columnList = "plate"
                ),
                @Index(
                        name = "idx_vehicle_history_renavam",
                        columnList = "renavam"
                ),
                @Index(
                        name = "idx_vehicle_history_deleted_at",
                        columnList = "deleted_at"
                )
        }
)
public class VehicleHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_id", nullable = false)
    private Long vehicleId;

    @Column
    private String nickname;

    @Column
    private String photo;

    @Column(nullable = false)
    private String plate;

    @Column(nullable = false)
    private String renavam;

    @Column(nullable = false)
    private String brand;

    @Column(nullable = false)
    private String model;

    @Column(name = "manufacturing_year", nullable = false)
    private int manufacturingYear;

    @Column(name = "model_year", nullable = false)
    private int modelYear;

    @Column
    private String color;

    @Column(name = "seating_capacity")
    private int seatingCapacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuel_type", nullable = false)
    private FuelType fuelType;

    @Column(name = "engine_displacement")
    private double engineDisplacement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleStatus status;

    @Column
    private Boolean towing;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "deleted_by_user_id", nullable = false)
    private Long deletedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "disabled_at")
    private Instant disabledAt;

    @Column(name = "deleted_at", nullable = false, updatable = false)
    private Instant deletedAt;
}
