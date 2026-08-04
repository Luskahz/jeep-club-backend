package com.jeepclub.backend.vehicles.api.http.dto.detailforedit;

import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Dados do veículo usados para pré-preencher o formulário de edição")
public class DetailForEditResponseDTO {

    @Schema(description = "Identificador único do veículo", example = "42")
    private Long id;

    @Schema(description = "Apelido informal do veículo", example = "Jipe do João", nullable = true)
    private String nickname;

    @Schema(description = "URL pública da foto do veículo", example = "https://example.com/foto.jpg", nullable = true)
    private String photo;

    @Schema(description = "Placa no formato Mercosul (ABC1D23) ou antigo (ABC1234)", example = "ABC1D23")
    private String plate;

    @Schema(description = "RENAVAM do veículo (9 a 11 dígitos)", example = "12345678901")
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

    @Schema(description = "Indica se o veículo possui capacidade de reboque", example = "true", nullable = true)
    private Boolean towing;

    public static DetailForEditResponseDTO from(Vehicle vehicle) {
        return DetailForEditResponseDTO.builder()
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
                .towing(vehicle.getTowing())
                .build();
    }
}
