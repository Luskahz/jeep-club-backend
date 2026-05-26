package com.jeepclub.backend.billing.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Dados para geração de um ciclo de cobrança.")
public record GenerateChargeCycleRequest(

        @NotBlank(message = "Código do ciclo é obrigatório.")
        @Size(max = 80, message = "Código do ciclo deve ter no máximo 80 caracteres.")
        @Schema(
                description = "Código identificador do ciclo de cobrança.",
                example = "2026",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String code,

        @NotNull(message = "Data de vencimento é obrigatória.")
        @FutureOrPresent(message = "Data de vencimento não pode estar no passado.")
        @Schema(
                description = "Data de vencimento das cobranças geradas no ciclo.",
                example = "2026-02-10",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        LocalDate dueDate
) {
}