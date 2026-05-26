package com.jeepclub.backend.billing.api.dto;

import com.jeepclub.backend.billing.core.application.result.GenerateChargeCycleResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

@Schema(description = "Resposta da geração de ciclo de cobrança.")
public record GenerateChargeCycleResponse(

        @Schema(description = "Ciclo de cobrança gerado.")
        ChargeCycleResponse chargeCycle,

        @Schema(description = "Quantidade de cobranças de membro criadas.", example = "35")
        int createdMemberCharges
) {

    public static GenerateChargeCycleResponse from(GenerateChargeCycleResult result) {
        Objects.requireNonNull(result, "result cannot be null");

        return new GenerateChargeCycleResponse(
                ChargeCycleResponse.from(result.chargeCycle()),
                result.createdMemberCharges()
        );
    }
}