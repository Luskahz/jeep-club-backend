package com.jeepclub.backend.vehicles.infra.persistence.jpa;

import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import com.jeepclub.backend.vehicles.infra.persistence.entity.VehicleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface VechileJpaRepository extends JpaRepository<VehicleEntity, Long> {



    boolean existsByPlate(String plate);

    boolean existsByRenavam(String renavam);

    boolean existsById(Long id);

    Optional<VehicleEntity> findByIdAndOwnerId(Long id, Long ownerId);
}
