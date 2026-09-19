package com.jeepclub.backend.vehicles.infra.persistence.adapter;

import com.jeepclub.backend.vehicles.core.application.exceptions.VehiclePlateAlreadyExistsException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleRenavamAlreadyExistsException;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import com.jeepclub.backend.vehicles.core.domain.exception.VehicleAlreadyDeletedException;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import com.jeepclub.backend.vehicles.core.repository.VehicleRepository;
import com.jeepclub.backend.vehicles.infra.persistence.entity.VehicleEntity;
import com.jeepclub.backend.vehicles.infra.persistence.entity.VehicleHistoryEntity;
import com.jeepclub.backend.vehicles.infra.persistence.jpa.VehicleHistoryJpaRepository;
import com.jeepclub.backend.vehicles.infra.persistence.jpa.VehicleJpaRepository;
import com.jeepclub.backend.vehicles.infra.persistence.mapper.VehicleHistoryMapper;
import com.jeepclub.backend.vehicles.infra.persistence.mapper.VehicleMapper;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class VehicleRepositoryAdapter implements VehicleRepository {

    private static final String PLATE_CONSTRAINT = "uk_vehicle_plate";
    private static final String RENAVAM_CONSTRAINT = "uk_vehicle_renavam";

    private final VehicleJpaRepository jpaRepository;
    private final VehicleHistoryJpaRepository historyJpaRepository;
    private final VehicleHistoryMapper historyMapper;

    @Override
    public Vehicle save(Vehicle vehicle) {
        VehicleEntity entity = VehicleMapper.toEntity(vehicle);
        try {
            VehicleEntity saved = jpaRepository.saveAndFlush(entity);
            return VehicleMapper.toDomain(saved);
        } catch (DataIntegrityViolationException exception) {
            if (violatesConstraint(exception, PLATE_CONSTRAINT)) {
                throw new VehiclePlateAlreadyExistsException(
                        "The license PLATE provided is already registered.",
                        exception
                );
            }
            if (violatesConstraint(exception, RENAVAM_CONSTRAINT)) {
                throw new VehicleRenavamAlreadyExistsException(
                        "The RENAVAM number provided is already registered.",
                        exception
                );
            }
            throw exception;
        }
    }

    private boolean violatesConstraint(Throwable exception, String constraintName) {
        Throwable cause = exception;

        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                String name = violation.getConstraintName();
                return name != null
                        && name.toLowerCase(Locale.ROOT).contains(constraintName);
            }
            cause = cause.getCause();
        }

        return false;
    }

    @Override
    public void delete(
            Vehicle vehicle,
            Long deletedByUserId,
            Instant deletedAt
    ) {
        VehicleEntity entity = jpaRepository
                .findByIdForUpdate(vehicle.getId())
                .orElseThrow(
                        () -> new VehicleAlreadyDeletedException(
                                vehicle.getId()
                        )
                );

        VehicleHistoryEntity history = historyMapper.toHistoryEntity(
                entity,
                deletedByUserId,
                deletedAt
        );

        historyJpaRepository.save(history);
        jpaRepository.delete(entity);
    }

    @Override
    public boolean existsByPlate(String plate) {
        return jpaRepository.existsByPlate(plate);
    }

    @Override
    public Optional<Vehicle> findById(Long id) {
        return jpaRepository.findById(id)
                .map(VehicleMapper::toDomain);
    }

    @Override
    public boolean existsByRenavam(String renavam) {
        return jpaRepository.existsByRenavam(renavam);
    }

    @Override
    public Optional<Vehicle> findByIdAndOwnerId(Long vehicleId, Long memberId) {
        return jpaRepository.findByIdAndOwnerId(vehicleId, memberId)
                .map(VehicleMapper::toDomain);
    }

    @Override
    public boolean existsById(Long vehicleId) {
        return jpaRepository.existsById(vehicleId);
    }

    @Override
    public Page<Vehicle> findAllByOwnerIdAndStatus(Long ownerId, VehicleStatus status, Pageable pageable) {
        return jpaRepository.findAllByOwnerIdAndStatus(ownerId, status, pageable)
                .map(VehicleMapper::toDomain);
    }

    @Override
    public Page<Vehicle> findAllByStatus(VehicleStatus status, Pageable pageable) {
        return jpaRepository.findAllByStatus(status, pageable)
                .map(VehicleMapper::toDomain);
    }
}
