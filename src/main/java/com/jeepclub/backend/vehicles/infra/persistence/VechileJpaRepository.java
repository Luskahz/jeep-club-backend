package com.jeepclub.backend.vehicles.infra.persistence;

import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;


public interface VechileJpaRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByPlate(String plate);

    boolean existsByRenavam(String renavam);
}
