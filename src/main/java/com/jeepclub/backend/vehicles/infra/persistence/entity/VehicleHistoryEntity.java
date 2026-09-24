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

    @Column(length = 100)
    private String nickname;

    @Column(name = "photo_storage_key", length = 255)
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

    @Column(name = "deleted_by_user_id", nullable = false)
    private Long deletedByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at", nullable = false, updatable = false)
    private Instant deletedAt;
}
