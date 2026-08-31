package com.jeepclub.backend.vehicles.infra.persistence.jpa;

import com.jeepclub.backend.vehicles.infra.persistence.entity.VehicleHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleHistoryJpaRepository
        extends JpaRepository<VehicleHistoryEntity, Long> {
}
