package com.jeepclub.backend.vehicles.api.dto.list;

import com.jeepclub.backend.vehicles.core.domain.model.Vehicle;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resumo de um veículo retornado em listagens")
public record ListResponseDTO(

        @Schema(description = "Identificador único do veículo", example = "42")
        Long id,

        @Schema(description = "Apelido informal do veículo", example = "Jipe do João", nullable = true)
        String nickname,

        @Schema(description = "Placa no formato Mercosul (ABC1D23) ou antigo (ABC1234)", example = "ABC1D23")
        String plate,

        @Schema(description = "URL pública da foto do veículo", example = "https://example.com/foto.jpg", nullable = true)
        String photo,

        @Schema(description = "Ano do modelo do veículo", example = "2022")
        int modelYear,

        @Schema(description = "Modelo do veículo", example = "Wrangler")
        String model,

        @Schema(description = "Cor predominante do veículo", example = "Preto")
        String color

) {
    public static ListResponseDTO from(Vehicle vehicle) {
        return new ListResponseDTO(
                vehicle.getId(),
                vehicle.getNickname(),
                vehicle.getPlate(),
                vehicle.getPhoto(),
                vehicle.getModelYear(),
                vehicle.getModel(),
                vehicle.getColor()
        );
    }
}