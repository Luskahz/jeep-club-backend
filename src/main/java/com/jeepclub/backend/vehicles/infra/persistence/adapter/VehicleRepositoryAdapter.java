package com.jeepclub.backend.vehicles.infra.persistence.adapter;

import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import com.jeepclub.backend.vehicles.core.repository.VehicleRepository;
import com.jeepclub.backend.vehicles.infra.persistence.entity.VehicleEntity;
import com.jeepclub.backend.vehicles.infra.persistence.jpa.VechileJpaRepository;
import com.jeepclub.backend.vehicles.infra.persistence.mapper.VehicleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@RequiredArgsConstructor
@Repository
public class VehicleRepositoryAdapter implements VehicleRepository {

    private final VechileJpaRepository jpaRepository;

    @Override
    public Vehicle save(Vehicle vehicle) {
        VehicleEntity entity = VehicleMapper.toEntity(vehicle);
        VehicleEntity saved = jpaRepository.save(entity);
        return VehicleMapper.toDomain(saved);
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
