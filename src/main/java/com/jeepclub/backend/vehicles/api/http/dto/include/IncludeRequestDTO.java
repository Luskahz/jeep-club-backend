package com.jeepclub.backend.vehicles.api.http.dto.include;

import com.jeepclub.backend.vehicles.api.http.validation.ValidRenavam;
import com.jeepclub.backend.vehicles.core.domain.enums.FuelType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Dados do cadastro de um novo veículo.")
public record IncludeRequestDTO(

        @Schema(
                description = "Nome personalizado ou apelido que o membro dá para o próprio veículo.",
                example = "Trovão Azul",
                maxLength = 100,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Size(max = 100, message = "O apelido deve ter no máximo 100 caracteres.")
        String nickname,

        @Schema(
                description = "URL ou caminho da foto única do veículo.",
                example = "https://jeepclub.com",
                maxLength = 255,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Size(max = 255, message = "O caminho da foto deve ter no máximo 255 caracteres.")
        String photo,

        @Schema(
                description = "Placa do veículo, no padrão Mercosul (ABC1D23) ou antigo (ABC1234). "
                        + "Espaços nas bordas e minúsculas são aceitos; o valor é persistido e "
                        + "consultado na forma canônica (trim + maiúsculas).",
                example = "ABC1D23",
                pattern = "^\\s*[A-Za-z]{3}[0-9][A-Za-z0-9][0-9]{2}\\s*$",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "A placa é obrigatória.")
        @Pattern(
                regexp = "^\\s*[A-Za-z]{3}[0-9][A-Za-z0-9][0-9]{2}\\s*$",
                message = "Formato de placa inválido. Use o padrão Mercosul (ABC1D23) ou Antigo (ABC1234)."
        )
        String plate,

        @Schema(
                description = "Código RENAVAM do veículo (específico para o Brasil). Pontuação "
                        + "entre os dígitos é aceita; o valor é persistido e consultado somente "
                        + "com dígitos, após validação do dígito verificador.",
                example = "38249206428",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "O RENAVAM é obrigatório.")
        @ValidRenavam
        String renavam,

        @Schema(
                description = "Marca do veículo.",
                example = "Jeep",
                maxLength = 50,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "A marca é obrigatória.")
        @Size(max = 50, message = "A marca deve ter no máximo 50 caracteres.")
        String brand,

        @Schema(
                description = "Modelo do veículo.",
                example = "Wrangler Rubicon",
                maxLength = 50,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "O modelo é obrigatório.")
        @Size(max = 50, message = "O modelo deve ter no máximo 50 caracteres.")
        String model,

        @Schema(
                description = "Ano de fabricação do veículo.",
                example = "2023",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @Min(value = 1900, message = "Ano de fabricação inválido.")
        int manufacturingYear,

        @Schema(
                description = "Ano do modelo do veículo.",
                example = "2024",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @Min(value = 1900, message = "Ano do modelo inválido.")
        int modelYear,

        @Schema(
                description = "Cor predominante do veículo.",
                example = "Verde Militar",
                maxLength = 30,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "A cor é obrigatória.")
        @Size(max = 30, message = "A cor deve ter no máximo 30 caracteres.")
        String color,

        @Schema(
                description = "Capacidade total de passageiros sentados.",
                example = "5",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @Min(value = 1, message = "A capacidade deve ser de pelo menos 1 passageiro.")
        int seatingCapacity,

        @Schema(
                description = "Tipo de combustível utilizado pelo veículo.",
                example = "DIESEL",
                allowableValues = {"GASOLINE", "ETHANOL", "FLEX", "DIESEL", "ELECTRIC", "HYBRID"},
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "O tipo de combustível é obrigatório.")
        FuelType fuelType,

        @Schema(
                description = "Cilindrada ou motorização do veículo.",
                example = "2.0",
                minimum = "0",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @Min(value = 0, message = "A cilindrada não pode ser negativa.")
        double engineDisplacement,

        @Schema(
                description = "Indica se o veículo possui guincho para reboque instalado.",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "A informação sobre o guincho/reboque é obrigatória.")
        Boolean towing



) {
}
