package com.jeepclub.backend.vehicles.infra.persistence.adapter;

import com.jeepclub.backend.authentication.core.domain.model.User;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import com.jeepclub.backend.vehicles.core.repository.VehicleRepository;
import com.jeepclub.backend.vehicles.infra.persistence.entity.VehicleEntity;
import com.jeepclub.backend.vehicles.infra.persistence.jpa.VechileJpaRepository;
import com.jeepclub.backend.vehicles.infra.persistence.mapper.VehicleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
    public boolean existsByRenavam(String renavam) {
        return jpaRepository.existsByRenavam(renavam);
    }
}
