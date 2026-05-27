package com.jeepclub.backend.billing.api.dto.definition;

import com.jeepclub.backend.billing.core.application.result.ChargeDefinitionResult;
import com.jeepclub.backend.billing.core.domain.enums.ChargeDefinitionStatus;
import com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Objects;

@Schema(description = "Resumo de uma definição de cobrança para listagem.")
public record ChargeDefinitionSummaryResponse(

        @Schema(description = "Identificador da definição de cobrança.", example = "1")
        Long id,

        @Schema(description = "Nome da cobrança.", example = "Anuidade")
        String name,

        @Schema(description = "Valor padrão da cobrança.", example = "250.00")
        BigDecimal defaultAmount,

        @Schema(description = "Tipo de recorrência da cobrança.", example = "YEARLY")
        ChargeRecurrenceType recurrenceType,

        @Schema(description = "Indica se a cobrança é obrigatória.", example = "true")
        Boolean required,

        @Schema(description = "Status da definição de cobrança.", example = "ACTIVE")
        ChargeDefinitionStatus status
) {

    public static ChargeDefinitionSummaryResponse from(ChargeDefinitionResult result) {
        Objects.requireNonNull(result, "result cannot be null");

        return new ChargeDefinitionSummaryResponse(
                result.id(),
                result.name(),
                result.defaultAmount(),
                result.recurrenceType(),
                result.required(),
                result.status()
        );
    }
}