package com.jeepclub.backend.vehicles.core.application.services;

import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleIdNotFoundException;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import com.jeepclub.backend.vehicles.core.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeleteService {

    private final VehicleRepository vehicleRepository;

    public void execute(Long vehicleId, Long memberId) {
        Vehicle vehicle = vehicleRepository
                .findByIdAndOwnerId(vehicleId, memberId)
                .orElseThrow(() -> new VehicleIdNotFoundException("Vehicle not found."));

        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw new VehicleIdNotFoundException("Vehicle not found.");
        }

        vehicleRepository.save(vehicle.softDelete());
    }

    public void executeAsAdmin(Long vehicleId) {
        Vehicle vehicle = vehicleRepository
                .findById(vehicleId)
                .orElseThrow(() -> new VehicleIdNotFoundException("Vehicle not found."));

        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw new VehicleIdNotFoundException("Vehicle not found.");
        }

        vehicleRepository.save(vehicle.softDelete());
    }
}