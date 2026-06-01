package com.jeepclub.backend.vehicles.core.application.services;

import com.jeepclub.backend.vehicles.api.dto.edit.EditRequestDTO;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleIdNotFoundException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehiclePlateAlreadyExistsException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleRenavamAlreadyExistsException;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import com.jeepclub.backend.vehicles.core.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EditService {

    private final VehicleRepository vehicleRepository;

    public void execute(Long vehicleId, Long memberId, EditRequestDTO dto) {
        Vehicle vehicle = vehicleRepository
                .findByIdAndOwnerId(vehicleId, memberId)
                .orElseThrow(() -> new VehicleIdNotFoundException("Vehicle not found."));

        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw new VehicleIdNotFoundException("Vehicle not found.");
        }

        if (!vehicle.getPlate().equals(dto.plate()) && vehicleRepository.existsByPlate(dto.plate())) {
            throw new VehiclePlateAlreadyExistsException("The license PLATE provided is already registered.");
        }

        if (!vehicle.getRenavam().equals(dto.renavam()) && vehicleRepository.existsByRenavam(dto.renavam())) {
            throw new VehicleRenavamAlreadyExistsException("The RENAVAM number provided is already registered.");
        }

        vehicle.update(
                dto.nickname(), dto.photo(), dto.plate(), dto.renavam(),
                dto.brand(), dto.model(), dto.manufacturingYear(), dto.modelYear(),
                dto.color(), dto.seatingCapacity(), dto.fuelType(), dto.engineDisplacement()
        );

        vehicleRepository.save(vehicle);
    }

    public void executeAsAdmin(Long vehicleId, EditRequestDTO dto) {
        Vehicle vehicle = vehicleRepository
                .findById(vehicleId)
                .orElseThrow(() -> new VehicleIdNotFoundException("Vehicle not found."));

        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw new VehicleIdNotFoundException("Vehicle not found.");
        }

        if (!vehicle.getPlate().equals(dto.plate()) && vehicleRepository.existsByPlate(dto.plate())) {
            throw new VehiclePlateAlreadyExistsException("The license PLATE provided is already registered.");
        }

        if (!vehicle.getRenavam().equals(dto.renavam()) && vehicleRepository.existsByRenavam(dto.renavam())) {
            throw new VehicleRenavamAlreadyExistsException("The RENAVAM number provided is already registered.");
        }

        vehicle.update(
                dto.nickname(), dto.photo(), dto.plate(), dto.renavam(),
                dto.brand(), dto.model(), dto.manufacturingYear(), dto.modelYear(),
                dto.color(), dto.seatingCapacity(), dto.fuelType(), dto.engineDisplacement()
        );

        vehicleRepository.save(vehicle);
    }
}
