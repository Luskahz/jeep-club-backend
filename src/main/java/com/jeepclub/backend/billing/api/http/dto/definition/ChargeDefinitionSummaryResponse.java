package com.jeepclub.backend.billing.api.http.dto.definition;

import com.jeepclub.backend.billing.core.application.result.ChargeDefinitionResult;
import com.jeepclub.backend.billing.core.domain.enums.definition.ChargeDefinitionStatus;
import com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType;
import com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy;
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

        @Schema(description = "Política de aceitação de pagamento.", example = "AFTER_DUE_DATE")
        PaymentAcceptancePolicy paymentAcceptancePolicy,

        @Schema(description = "Dias de tolerância para pagamento após vencimento.", example = "15", nullable = true)
        Integer latePaymentGraceDays,

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
                result.paymentAcceptancePolicy(),
                result.latePaymentGraceDays(),
                result.status()
        );
    }
}