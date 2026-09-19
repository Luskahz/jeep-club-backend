package com.jeepclub.backend.vehicles.api.http.dto.edit;

import com.jeepclub.backend.vehicles.api.http.validation.ValidRenavam;
import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Payload de atualização parcial dos dados editáveis do veículo. Um campo "
        + "omitido preserva o valor atual. Nickname, photo e color podem ser "
        + "limpos enviando null explícito. Plate, renavam, brand, model, "
        + "manufacturingYear, modelYear, seatingCapacity, fuelType e engineDisplacement "
        + "não podem ser limpos: se enviados como null, o request é rejeitado com 400.")
public record EditRequestDTO(

        @Schema(
                description = "Apelido informal do veículo. Omitir preserva o valor atual; "
                        + "null explícito limpa o campo.",
                example = "Jipe do João",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                maxLength = 50
        )
        @Size(max = 50)
        String nickname,

        @Schema(
                description = "URL pública da foto do veículo. Omitir preserva o valor atual; "
                        + "null explícito limpa o campo.",
                example = "https://example.com/foto.jpg",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                maxLength = 500
        )
        @Size(max = 500)
        String photo,

        @Schema(
                description = "Placa do veículo no formato Mercosul (ABC1D23) ou antigo (ABC1234). "
                        + "Espaços nas bordas e minúsculas são aceitos; o valor é persistido e "
                        + "consultado na forma canônica (trim + maiúsculas). Omitir preserva o "
                        + "valor atual; o campo não pode ser limpo, então null explícito é "
                        + "rejeitado com 400.",
                example = "ABC1D23",
                nullable = false,
                pattern = "^\\s*[A-Za-z]{3}[0-9][A-Za-z0-9][0-9]{2}\\s*$",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Pattern(
                regexp = "^\\s*[A-Za-z]{3}[0-9][A-Za-z0-9][0-9]{2}\\s*$",
                message = "Placa inválida"
        )
        String plate,

        @Schema(
                description = "RENAVAM válido com 11 dígitos ou no formato documentado "
                        + "###.###.###-##. O valor é persistido e consultado somente com dígitos, após validação do dígito "
                        + "verificador. Omitir preserva o valor atual; o campo não pode ser "
                        + "limpo, então null explícito é rejeitado com 400.",
                example = "38249206428",
                nullable = false,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @ValidRenavam
        String renavam,

        @Schema(
                description = "Marca do fabricante. Omitir preserva o valor atual; o campo não "
                        + "pode ser limpo, então null explícito é rejeitado com 400.",
                example = "Jeep",
                nullable = false,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                maxLength = 50
        )
        @Size(max = 50)
        String brand,

        @Schema(
                description = "Modelo do veículo. Omitir preserva o valor atual; o campo não "
                        + "pode ser limpo, então null explícito é rejeitado com 400.",
                example = "Wrangler",
                nullable = false,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                maxLength = 100
        )
        @Size(max = 100)
        String model,

        @Schema(
                description = "Ano de fabricação do veículo. Omitir preserva o valor atual; o "
                        + "campo não pode ser limpo, então null explícito é rejeitado com 400.",
                example = "2021",
                nullable = false,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                minimum = "1900",
                maximum = "2100"
        )
        @Min(value = 1900, message = "Ano de fabricação inválido")
        @Max(value = 2100, message = "Ano de fabricação inválido")
        Integer manufacturingYear,

        @Schema(
                description = "Ano do modelo do veículo (pode ser diferente do ano de "
                        + "fabricação). Omitir preserva o valor atual; o campo não pode ser "
                        + "limpo, então null explícito é rejeitado com 400.",
                example = "2022",
                nullable = false,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                minimum = "1900",
                maximum = "2100"
        )
        @Min(value = 1900, message = "Ano do modelo inválido")
        @Max(value = 2100, message = "Ano do modelo inválido")
        Integer modelYear,

        @Schema(
                description = "Cor predominante do veículo. Omitir preserva o valor atual; "
                        + "null explícito limpa o campo.",
                example = "Preto",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                maxLength = 30
        )
        @Size(max = 30)
        String color,

        @Schema(
                description = "Número de lugares do veículo, incluindo o motorista. Omitir "
                        + "preserva o valor atual; o campo não pode ser limpo, então null "
                        + "explícito é rejeitado com 400.",
                example = "5",
                nullable = false,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                minimum = "1",
                maximum = "50"
        )
        @Min(value = 1, message = "Capacidade mínima é 1")
        @Max(value = 50, message = "Capacidade máxima é 50")
        Integer seatingCapacity,

        @Schema(
                description = "Tipo de combustível aceito pelo veículo. Omitir preserva o valor "
                        + "atual; o campo não pode ser limpo, então null explícito é rejeitado "
                        + "com 400.",
                example = "GASOLINE",
                nullable = false,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                allowableValues = {"GASOLINE", "ETHANOL", "DIESEL", "FLEX", "ELECTRIC", "HYBRID"}
        )
        FuelType fuelType,

        @Schema(
                description = "Cilindrada do motor em litros. Omitir preserva o valor atual; o "
                        + "campo não pode ser limpo, então null explícito é rejeitado com 400. "
                        + "Zero é um valor válido e é aplicado normalmente quando enviado.",
                example = "2.0",
                nullable = false,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED,
                minimum = "0.0"
        )
        @Min(value = 0, message = "Cilindrada inválida")
        Double engineDisplacement,

        @Schema(
                description = "Indica se o veículo possui guincho/reboque. Omitir preserva o "
                        + "valor atual; null explícito é rejeitado; false é aplicado "
                        + "normalmente quando enviado.",
                example = "true",
                nullable = false,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        Boolean towing
) {}
