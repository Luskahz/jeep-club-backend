package com.jeepclub.backend.vehicles.core.application;

import com.jeepclub.backend.vehicles.core.application.exceptions.VehiclePlateAlreadyExistsException;
import com.jeepclub.backend.vehicles.core.application.exceptions.VehicleRenavamAlreadyExistsException;
import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import com.jeepclub.backend.vehicles.core.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class IncludeService {

    private final VehicleRepository vehicleRepository;

    public Vehicle includeVehicle(
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
        Instant now = Instant.now();

        if (vehicleRepository.existsByPlate(plate)) {
            throw new VehiclePlateAlreadyExistsException("The license plate provided is already registered.");
        }

        if (vehicleRepository.existsByRenavam(renavam)) {
            throw new VehicleRenavamAlreadyExistsException("The RENAVAM number provided is already registered.");
        }

        Vehicle newVehicle = new Vehicle(
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
                ownerId

        );

        return vehicleRepository.save(newVehicle);
    }
}
