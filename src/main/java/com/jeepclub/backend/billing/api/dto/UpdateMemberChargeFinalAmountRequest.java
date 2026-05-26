package com.jeepclub.backend.billing.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Dados para atualização do valor final de uma cobrança de membro.")
public record UpdateMemberChargeFinalAmountRequest(

        @NotNull(message = "Valor final é obrigatório.")
        @DecimalMin(value = "0.01", message = "Valor final deve ser maior que zero.")
        @Schema(
                description = "Novo valor final da cobrança.",
                example = "200.00",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        BigDecimal finalAmount
) {
}