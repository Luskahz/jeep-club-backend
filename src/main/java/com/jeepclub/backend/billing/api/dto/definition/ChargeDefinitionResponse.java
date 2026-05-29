package com.jeepclub.backend.billing.api.dto.definition;

import com.jeepclub.backend.billing.core.application.result.ChargeDefinitionResult;
import com.jeepclub.backend.billing.core.domain.enums.ChargeDefinitionStatus;
import com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType;
import com.jeepclub.backend.billing.core.domain.enums.PaymentAcceptancePolicy;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Schema(description = "Resposta com os dados de uma definição de cobrança.")
public record ChargeDefinitionResponse(

        @Schema(description = "Identificador da definição de cobrança.", example = "1")
        Long id,

        @Schema(description = "Nome da cobrança.", example = "Anuidade")
        String name,

        @Schema(description = "Descrição da cobrança.", example = "Cobrança anual obrigatória dos membros do clube.", nullable = true)
        String description,

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
        ChargeDefinitionStatus status,

        @Schema(description = "Data de criação da definição de cobrança.")
        Instant createdAt,

        @Schema(description = "Data da última atualização da definição de cobrança.", nullable = true)
        Instant updatedAt
) {

    public static ChargeDefinitionResponse from(ChargeDefinitionResult result) {
        Objects.requireNonNull(result, "result cannot be null");

        return new ChargeDefinitionResponse(
                result.id(),
                result.name(),
                result.description(),
                result.defaultAmount(),
                result.recurrenceType(),
                result.required(),
                result.paymentAcceptancePolicy(),
                result.latePaymentGraceDays(),
                result.status(),
                result.createdAt(),
                result.updatedAt()
        );
    }

    public static List<ChargeDefinitionResponse> from(List<ChargeDefinitionResult> results) {
        Objects.requireNonNull(results, "results cannot be null");

        return results.stream()
                .map(ChargeDefinitionResponse::from)
                .toList();
    }
}