package com.jeepclub.backend.vehicles.core.repository;

import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import com.jeepclub.backend.vehicles.infra.persistence.entity.VehicleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleRepository {

    boolean existsByPlate(String plate);

    boolean existsByRenavam(String renavam);

    boolean existsById(Long id);

    Vehicle save(Vehicle vehicle);

    Optional<Vehicle> findByIdAndOwnerId(Long id, Long memberId);
}

