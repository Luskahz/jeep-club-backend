package com.jeepclub.backend.vehicles.api.dto.edit;

import com.jeepclub.backend.vehicles.api.validation.ValidRenavam;
import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import io.swagger.v3.oas.annotations.media.Schema;


// pendente mais infos do swagger, como o description
public record EditRequestDTO(

        @Schema(example = "Jipe do João")
        String nickname,

        @Schema(example = "https://example.com/foto.jpg")
        String photo,

        @Schema(example = "ABC1D23")
        String plate,

        @Schema(example = "12345678901")
        @ValidRenavam
        String renavam,

        @Schema(example = "Jeep")
        String brand,

        @Schema(example = "Wrangler")
        String model,

        @Schema(example = "2021")
        int manufacturingYear,

        @Schema(example = "2022")
        int modelYear,

        @Schema(example = "Preto")
        String color,

        @Schema(example = "5")
        int seatingCapacity,

        @Schema(example = "GASOLINE")
        FuelType fuelType,

        @Schema(example = "2.0")
        double engineDisplacement
) {}