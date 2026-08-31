package com.jeepclub.backend.vehicles.core.application.service.vehicle;

import com.jeepclub.backend.vehicles.core.application.exceptions.UserNotFoundException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleIdNotFoundException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehiclePlateAlreadyExistsException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleRenavamAlreadyExistsException;
import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import com.jeepclub.backend.vehicles.core.port.UserPort;
import com.jeepclub.backend.vehicles.core.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AdminVehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserPort userPort;
    private final Clock clock;

    @Transactional
    public Vehicle createForOwner(
            String nickname,
            String photo,
            String plate,
            String renavam,
            String brand,
            String model,
            int manufacturingYear,
            int modelYear,
            String color,
            int seatingCapacity,
            FuelType fuelType,
            double engineDisplacement,
            Boolean towing,
            Long ownerId
    ) {
        assertUniquePlateAndRenavam(plate, renavam);

        if (!userPort.existsById(ownerId)) {
            throw new UserNotFoundException("User id not found.");
        }

        Vehicle vehicle = Vehicle.create(
                nickname,
                photo,
                plate,
                renavam,
                brand,
                model,
                manufacturingYear,
                modelYear,
                color,
                seatingCapacity,
                fuelType,
                engineDisplacement,
                towing,
                ownerId,
                Instant.now(clock)
        );

        return vehicleRepository.save(vehicle);
    }

    @Transactional(readOnly = true)
    public Page<Vehicle> findAll(Pageable pageable) {
        return vehicleRepository.findAllByStatus(VehicleStatus.ACTIVE, pageable);
    }

    @Transactional(readOnly = true)
    public Vehicle findById(Long vehicleId) {
        return findActiveVehicle(vehicleId);
    }

    @Transactional
    public void update(
            Long vehicleId,
            String nickname,
            String photo,
            String plate,
            String renavam,
            String brand,
            String model,
            int manufacturingYear,
            int modelYear,
            String color,
            int seatingCapacity,
            FuelType fuelType,
            double engineDisplacement,
            boolean towing
    ) {
        Vehicle vehicle = findActiveVehicle(vehicleId);
        assertUniqueChangedIdentifiers(vehicle, plate, renavam);

        vehicle.update(
                nickname,
                photo,
                plate,
                renavam,
                brand,
                model,
                manufacturingYear,
                modelYear,
                color,
                seatingCapacity,
                fuelType,
                engineDisplacement,
                towing,
                Instant.now(clock)
        );

        vehicleRepository.save(vehicle);
    }

    @Transactional
    public void delete(
            Long vehicleId,
            Long deletedByUserId
    ) {
        Vehicle vehicle = findActiveVehicle(vehicleId);
        vehicleRepository.delete(
                vehicle,
                deletedByUserId,
                Instant.now(clock)
        );
    }

    private Vehicle findActiveVehicle(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new VehicleIdNotFoundException("Vehicle not found."));

        assertActive(vehicle);
        return vehicle;
    }

    private void assertUniquePlateAndRenavam(String plate, String renavam) {
        if (vehicleRepository.existsByPlate(plate)) {
            throw new VehiclePlateAlreadyExistsException(
                    "The license PLATE provided is already registered."
            );
        }

        if (vehicleRepository.existsByRenavam(renavam)) {
            throw new VehicleRenavamAlreadyExistsException(
                    "The RENAVAM number provided is already registered."
            );
        }
    }

    private void assertUniqueChangedIdentifiers(
            Vehicle vehicle,
            String plate,
            String renavam
    ) {
        if (!vehicle.getPlate().equals(plate) && vehicleRepository.existsByPlate(plate)) {
            throw new VehiclePlateAlreadyExistsException(
                    "The license PLATE provided is already registered."
            );
        }

        if (!vehicle.getRenavam().equals(renavam)
                && vehicleRepository.existsByRenavam(renavam)) {
            throw new VehicleRenavamAlreadyExistsException(
                    "The RENAVAM number provided is already registered."
            );
        }
    }

    private void assertActive(Vehicle vehicle) {
        if (vehicle.getStatus() != VehicleStatus.ACTIVE) {
            throw new VehicleIdNotFoundException("Vehicle not found.");
        }
    }
}
