package com.jeepclub.backend.vehicles.api.dto.detail;

import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Dados completos de um veículo")
public record DetailResponseDTO(

        @Schema(description = "ID do veículo", example = "1")
        Long id,

        @Schema(description = "Apelido do veículo", example = "Trovão Azul")
        String nickname,

        @Schema(description = "URL da foto do veículo", example = "https://jeepclub.com/foto.jpg")
        String photo,

        @Schema(description = "Placa do veículo", example = "ABC1D23")
        String plate,

        @Schema(description = "RENAVAM do veículo", example = "85236914706")
        String renavam,

        @Schema(description = "Marca do veículo", example = "Jeep")
        String brand,

        @Schema(description = "Modelo do veículo", example = "Wrangler Rubicon")
        String model,

        @Schema(description = "Ano de fabricação", example = "2023")
        int manufacturingYear,

        @Schema(description = "Ano do modelo", example = "2024")
        int modelYear,

        @Schema(description = "Cor predominante", example = "Verde Militar")
        String color,

        @Schema(description = "Capacidade de passageiros", example = "5")
        int seatingCapacity,

        @Schema(description = "Tipo de combustível", example = "DIESEL")
        FuelType fuelType,

        @Schema(description = "Cilindrada do motor", example = "2.0")
        double engineDisplacement,

        @Schema(description = "Status do veículo", example = "ACTIVE")
        VehicleStatus status,

        @Schema(description = "Possui reboque", example = "true")
        Boolean towing,

        @Schema(description = "ID do proprietário", example = "1")
        Long ownerId,

        @Schema(description = "Data de criação")
        Instant createdAt


) {
    public static DetailResponseDTO from(Vehicle vehicle) {
        return new DetailResponseDTO(
                vehicle.getId(),
                vehicle.getNickname(),
                vehicle.getPhoto(),
                vehicle.getPlate(),
                vehicle.getRenavam(),
                vehicle.getBrand(),
                vehicle.getModel(),
                vehicle.getManufacturingYear(),
                vehicle.getModelYear(),
                vehicle.getColor(),
                vehicle.getSeatingCapacity(),
                vehicle.getFuelType(),
                vehicle.getEngineDisplacement(),
                vehicle.getStatus(),
                vehicle.getTowing(),
                vehicle.getOwnerId(),
                vehicle.getCreatedAt()
        );
    }
}