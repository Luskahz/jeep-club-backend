package com.jeepclub.backend.vehicles.core.application.services;

import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleIdNotFoundException;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import com.jeepclub.backend.vehicles.core.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class DeleteService {

    private final VehicleRepository vehicleRepository;
    private final Clock clock;

    public void execute(Long vehicleId, Long memberId) {
        Instant now = Instant.now(clock);
        Vehicle vehicle = vehicleRepository
                .findByIdAndOwnerId(vehicleId, memberId)
                .orElseThrow(() -> new VehicleIdNotFoundException("Vehicle not found."));

        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw new VehicleIdNotFoundException("Vehicle not found.");
        }

        vehicleRepository.save(vehicle.softDelete(now));
    }

    public void executeAsAdmin(Long vehicleId) {
        Instant now = Instant.now(clock);
        Vehicle vehicle = vehicleRepository
                .findById(vehicleId)
                .orElseThrow(() -> new VehicleIdNotFoundException("Vehicle not found."));

        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw new VehicleIdNotFoundException("Vehicle not found.");
        }

        vehicleRepository.save(vehicle.softDelete(now));
    }
}