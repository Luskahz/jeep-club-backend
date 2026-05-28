package com.jeepclub.backend.billing.api.dto.cycle;

import com.jeepclub.backend.billing.core.application.result.cycle.ChargeCycleResult;
import com.jeepclub.backend.billing.core.domain.enums.ChargeCycleStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Schema(description = "Resumo de um ciclo de cobrança para listagem.")
public record ChargeCycleSummaryResponse(

        @Schema(description = "Identificador do ciclo.", example = "1")
        Long id,

        @Schema(description = "Identificador da definição de cobrança.", example = "10")
        Long chargeDefinitionId,

        @Schema(description = "Nome da definição de cobrança no momento em que o ciclo foi gerado.", example = "Anuidade Pesca 2026")
        String chargeDefinitionNameSnapshot,

        @Schema(description = "Código do ciclo.", example = "2026")
        String code,

        @Schema(description = "Data de vencimento das cobranças do ciclo.", example = "2026-02-10")
        LocalDate dueDate,

        @Schema(description = "Status do ciclo.", example = "GENERATED")
        ChargeCycleStatus status,

        @Schema(description = "Data em que o ciclo foi gerado.")
        Instant generatedAt
) {

    public static ChargeCycleSummaryResponse from(ChargeCycleResult result) {
        Objects.requireNonNull(result, "result cannot be null");

        return new ChargeCycleSummaryResponse(
                result.id(),
                result.chargeDefinitionId(),
                result.chargeDefinitionNameSnapshot(),
                result.code(),
                result.dueDate(),
                result.status(),
                result.generatedAt()
        );
    }
}