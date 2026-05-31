package com.jeepclub.backend.vehicles.api.dto.detail;

import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import lombok.Builder;
import lombok.Getter;
import java.time.Instant;

@Getter
@Builder
public class DetailResponseDTO {

        private Long id;
        private String nickname;
        private String photo;
        private String plate;
        private String renavam;
        private String brand;
        private String model;
        private int manufacturingYear;
        private int modelYear;
        private String color;
        private int seatingCapacity;
        private FuelType fuelType;
        private double engineDisplacement;
        private VehicleStatus status;
        private Boolean towing;
        private Long ownerId;
        private Instant createdAt;
        private Instant updatedAt;
        private Instant disabledAt;

        public static DetailResponseDTO from(Vehicle vehicle) {
                return DetailResponseDTO.builder()
                        .id(vehicle.getId())
                        .nickname(vehicle.getNickname())
                        .photo(vehicle.getPhoto())
                        .plate(vehicle.getPlate())
                        .renavam(vehicle.getRenavam())
                        .brand(vehicle.getBrand())
                        .model(vehicle.getModel())
                        .manufacturingYear(vehicle.getManufacturingYear())
                        .modelYear(vehicle.getModelYear())
                        .color(vehicle.getColor())
                        .seatingCapacity(vehicle.getSeatingCapacity())
                        .fuelType(vehicle.getFuelType())
                        .engineDisplacement(vehicle.getEngineDisplacement())
                        .status(vehicle.getStatus())
                        .towing(vehicle.getTowing())
                        .ownerId(vehicle.getOwnerId())
                        .createdAt(vehicle.getCreatedAt())
                        .updatedAt(vehicle.getUpdatedAt())
                        .disabledAt(vehicle.getDisabledAt())
                        .build();
        }
}