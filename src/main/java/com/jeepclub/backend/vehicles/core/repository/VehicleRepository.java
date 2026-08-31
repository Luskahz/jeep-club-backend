package com.jeepclub.backend.vehicles.core.repository;

import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;

public interface VehicleRepository {

    boolean existsByPlate(String plate);

    boolean existsByRenavam(String renavam);

    boolean existsById(Long id);

    Vehicle save(Vehicle vehicle);

    void delete(
            Vehicle vehicle,
            Long deletedByUserId,
            Instant deletedAt
    );

    Optional<Vehicle> findByIdAndOwnerId(Long id, Long memberId);

    Optional<Vehicle> findById(Long id);

    Page<Vehicle> findAllByOwnerIdAndStatus(Long ownerId, VehicleStatus status, Pageable pageable);

    Page<Vehicle> findAllByStatus(VehicleStatus status, Pageable pageable);
}
