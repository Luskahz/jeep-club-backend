package com.jeepclub.backend.vehicles.core.application.services;

import com.jeepclub.backend.vehicles.api.dto.detail.DetailResponseDTO;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleIdNotFoundException;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import com.jeepclub.backend.vehicles.core.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DetailService {

    private final VehicleRepository vehicleRepository;

    public DetailResponseDTO execute(Long vehicleId, Long memberId) {
        Vehicle vehicle = vehicleRepository
                .findByIdAndOwnerId(vehicleId, memberId)
                .orElseThrow(() -> new VehicleIdNotFoundException("Vehicle not id not found."));

        return DetailResponseDTO.from(vehicle);
    }
}