package com.jeepclub.backend.vehicles.api.http.dto.detail;

import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import com.jeepclub.backend.vehicles.core.domain.enums.VehicleStatus;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import java.time.Instant;

@Getter
@Builder
@Schema(description = "Dados completos de um veículo cadastrado no sistema")
public class DetailResponseDTO {

        @Schema(description = "Identificador único do veículo", example = "42")
        private Long id;

        @Schema(description = "Apelido informal do veículo", example = "Jipe do João", nullable = true)
        private String nickname;

        @Schema(description = "Chave da imagem no storage global (GET /media/images?key=...)", example = "images/2026/09/24/550e8400-e29b-41d4-a716-446655440000.jpg", nullable = true)
        private String photo;

        @Schema(description = "Placa no formato Mercosul (ABC1D23) ou antigo (ABC1234)", example = "ABC1D23")
        private String plate;

        @Schema(description = "RENAVAM conforme persistido no cadastro", example = "38249206428")
        private String renavam;

        @Schema(description = "Marca do fabricante", example = "Jeep")
        private String brand;

        @Schema(description = "Modelo do veículo", example = "Wrangler")
        private String model;

        @Schema(description = "Ano de fabricação", example = "2021")
        private int manufacturingYear;

        @Schema(description = "Ano do modelo (pode diferir do ano de fabricação)", example = "2022")
        private int modelYear;

        @Schema(description = "Cor predominante do veículo", example = "Preto")
        private String color;

        @Schema(description = "Número de lugares incluindo o motorista", example = "5")
        private int seatingCapacity;

        @Schema(
                description = "Tipo de combustível aceito pelo veículo",
                example = "GASOLINE",
                allowableValues = {"GASOLINE", "ETHANOL", "DIESEL", "FLEX", "ELECTRIC", "HYBRID"}
        )
        private FuelType fuelType;

        @Schema(description = "Cilindrada do motor em litros", example = "2.0")
        private double engineDisplacement;

        @Schema(
                description = "Status persistido do veículo. A única política de exclusão vigente é hard "
                        + "delete com histórico; nenhuma rota atual produz ou expõe outro valor.",
                example = "ACTIVE",
                allowableValues = {"ACTIVE"}
        )
        private VehicleStatus status;

        @Schema(description = "Indica se o veículo possui capacidade de reboque", example = "true", nullable = true)
        private Boolean towing;

        @Schema(description = "ID do proprietário do veículo", example = "7")
        private Long ownerId;

        @Schema(description = "Data e hora de cadastro do veículo (UTC)", example = "2024-03-15T10:30:00Z")
        private Instant createdAt;

        @Schema(description = "Data e hora da última atualização (UTC)", example = "2024-06-01T08:00:00Z", nullable = true)
        private Instant updatedAt;

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
                        .build();
        }
}
