package com.jeepclub.backend.vehicles.infra.persistence.jpa;

import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import com.jeepclub.backend.vehicles.infra.persistence.entity.VehicleEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VehicleJpaRepository extends JpaRepository<VehicleEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select v
            from VehicleEntity v
            where v.id = :id
            """)
    Optional<VehicleEntity> findByIdForUpdate(
            @Param("id") Long id
    );

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
