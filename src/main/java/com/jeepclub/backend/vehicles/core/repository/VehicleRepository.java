package com.jeepclub.backend.vehicles.core.repository;

import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository {

    boolean existsByPlate(String plate);

    boolean existsByRenavam(String renavam);

    Vehicle save(Vehicle vehicle);
}
