package com.jeepclub.backend.vehicles.infra.persistence.jpa;

import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import com.jeepclub.backend.vehicles.infra.persistence.entity.VehicleEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleJpaRepository extends JpaRepository<VehicleEntity, Long> {

    boolean existsByPlate(String plate);

    boolean existsByRenavam(String renavam);

    Optional<VehicleEntity> findByIdAndOwnerId(Long id, Long ownerId);

    Page<VehicleEntity> findAllByOwnerIdAndStatus(
            Long ownerId,
            VehicleStatus status,
            Pageable pageable
    );

    Page<VehicleEntity> findAllByStatus(VehicleStatus status, Pageable pageable);
}
