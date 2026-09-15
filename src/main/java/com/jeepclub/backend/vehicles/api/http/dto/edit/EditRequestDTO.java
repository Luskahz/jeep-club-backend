package com.jeepclub.backend.vehicles.api.http.dto.edit;

import com.jeepclub.backend.vehicles.api.http.validation.ValidRenavam;
import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload de substituição dos dados editáveis do veículo. A implementação atual não possui semântica parcial: campos de referência omitidos chegam como null e qualquer primitive omitida é rejeitada durante a leitura do JSON.")
public record EditRequestDTO(

        @Schema(
                description = "Apelido informal do veículo",
                example = "Jipe do João",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                maxLength = 50
        )
        @Size(max = 50)
        String nickname,

        @Schema(
                description = "URL pública da foto do veículo",
                example = "https://example.com/foto.jpg",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                maxLength = 500
        )
        @Size(max = 500)
        String photo,

        @Schema(
                description = "Placa do veículo no formato Mercosul (ABC1D23) ou antigo (ABC1234)",
                example = "ABC1D23",
                nullable = false,
                pattern = "^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @Pattern(regexp = "^[A-Z]{3}[0-9][A-Z0-9][0-9]{2}$", message = "Placa inválida")
        String plate,

        @Schema(
                description = "RENAVAM válido. Na edição, o checksum ignora caracteres não numéricos, mas o texto recebido é encaminhado sem normalização para persistência.",
                example = "38249206428",
                nullable = false,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @ValidRenavam
        String renavam,

        @Schema(
                description = "Marca do fabricante",
                example = "Jeep",
                nullable = false,
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 50
        )
        @Size(max = 50)
        String brand,

        @Schema(
                description = "Modelo do veículo",
                example = "Wrangler",
                nullable = false,
                requiredMode = Schema.RequiredMode.REQUIRED,
                maxLength = 100
        )
        @Size(max = 100)
        String model,

        @Schema(
                description = "Ano de fabricação do veículo",
                example = "2021",
                nullable = false,
                requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "1900",
                maximum = "2100"
        )
        @Min(value = 1900, message = "Ano de fabricação inválido")
        @Max(value = 2100, message = "Ano de fabricação inválido")
        int manufacturingYear,

        @Schema(
                description = "Ano do modelo do veículo (pode ser diferente do ano de fabricação)",
                example = "2022",
                nullable = false,
                requiredMode = Schema.RequiredMode.REQUIRED,
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
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                maxLength = 30
        )
        @Size(max = 30)
        String color,

        @Schema(
                description = "Número de lugares do veículo, incluindo o motorista",
                example = "5",
                nullable = false,
                requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "1",
                maximum = "50"
        )
        @Min(value = 1, message = "Capacidade mínima é 1")
        @Max(value = 50, message = "Capacidade máxima é 50")
        int seatingCapacity,

        @Schema(
                description = "Tipo de combustível aceito pelo veículo",
                example = "GASOLINE",
                nullable = false,
                requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"GASOLINE", "ETHANOL", "DIESEL", "FLEX", "ELECTRIC", "HYBRID"}
        )
        FuelType fuelType,

        @Schema(
                description = "Cilindrada do motor em litros. O campo primitive deve estar presente no JSON.",
                example = "2.0",
                nullable = false,
                requiredMode = Schema.RequiredMode.REQUIRED,
                minimum = "0.0"
        )
        @Min(value = 0, message = "Cilindrada inválida")
        double engineDisplacement,

        @Schema(
                description = "Indica se o veículo possui guincho/reboque. O campo primitive deve estar presente no JSON.",
                example = "true",
                nullable = false,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        boolean towing
) {}
