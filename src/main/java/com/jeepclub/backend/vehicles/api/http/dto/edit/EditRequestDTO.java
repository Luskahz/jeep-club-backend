package com.jeepclub.backend.vehicles.api.http.dto.edit;

import com.jeepclub.backend.vehicles.api.http.validation.ValidRenavam;
import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload para edição parcial de um veículo. Apenas os campos enviados serão atualizados.")
public record EditRequestDTO(

        @Schema(
                description = "Apelido informal do veículo",
                example = "Jipe do João",
                nullable = true,
                maxLength = 50
        )
        @Size(max = 50)
        String nickname,

        @Schema(
                description = "URL pública da foto do veículo",
                example = "https://example.com/foto.jpg",
                nullable = true,
                maxLength = 500
        )
        @Size(max = 500)
        String photo,

        @Schema(
                description = "Placa do veículo no formato Mercosul (ABC1D23) ou antigo (ABC1234)",
                example = "ABC1D23",
                nullable = true,
                pattern = "^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$"
        )
        @Pattern(regexp = "^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$", message = "Placa inválida")
        String plate,

        @Schema(
                description = "RENAVAM do veículo (9 a 11 dígitos numéricos)",
                example = "12345678901",
                nullable = true,
                minLength = 9,
                maxLength = 11,
                pattern = "^\\d{9,11}$"
        )
        @ValidRenavam
        String renavam,

        @Schema(
                description = "Marca do fabricante",
                example = "Jeep",
                nullable = true,
                maxLength = 50
        )
        @Size(max = 50)
        String brand,

        @Schema(
                description = "Modelo do veículo",
                example = "Wrangler",
                nullable = true,
                maxLength = 100
        )
        @Size(max = 100)
        String model,

        @Schema(
                description = "Ano de fabricação do veículo",
                example = "2021",
                nullable = true,
                minimum = "1900",
                maximum = "2100"
        )
        @Min(value = 1900, message = "Ano de fabricação inválido")
        @Max(value = 2100, message = "Ano de fabricação inválido")
        int manufacturingYear,

        @Schema(
                description = "Ano do modelo do veículo (pode ser diferente do ano de fabricação)",
                example = "2022",
                nullable = true,
                minimum = "1900",
                maximum = "2100"
        )
        @Min(value = 1900, message = "Ano do modelo inválido")
        @Max(value = 2100, message = "Ano do modelo inválido")
        int modelYear,

        @Schema(
                description = "Cor predominante do veículo",
                example = "Preto",
                nullable = true,
                maxLength = 30
        )
        @Size(max = 30)
        String color,

        @Schema(
                description = "Número de lugares do veículo, incluindo o motorista",
                example = "5",
                nullable = true,
                minimum = "1",
                maximum = "50"
        )
        @Min(value = 1, message = "Capacidade mínima é 1")
        @Max(value = 50, message = "Capacidade máxima é 50")
        int seatingCapacity,

        @Schema(
                description = "Tipo de combustível aceito pelo veículo",
                example = "GASOLINE",
                nullable = true,
                allowableValues = {"GASOLINE", "ETHANOL", "DIESEL", "FLEX", "ELECTRIC", "HYBRID"}
        )
        FuelType fuelType,

        @Schema(
                description = "Cilindrada do motor em litros",
                example = "2.0",
                nullable = true,
                minimum = "0.0",
                maximum = "20.0"
        )
        @Min(value = 0, message = "Cilindrada inválida")
        double engineDisplacement,

        @Schema(
                description = "Indica se o veículo possui guincho/reboque",
                example = "true",
                nullable = true
        )
        boolean towing
) {}