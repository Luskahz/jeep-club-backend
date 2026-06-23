package com.jeepclub.backend.billing.api.http.dto.cycle;

import com.jeepclub.backend.billing.core.application.result.cycle.ChargeCycleResult;
import com.jeepclub.backend.billing.core.domain.enums.cycle.ChargeCycleStatus;
import com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy;
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

        @Schema(description = "Política de aceitação de pagamento no momento em que o ciclo foi gerado.", example = "AFTER_DUE_DATE")
        PaymentAcceptancePolicy chargeDefinitionPaymentAcceptancePolicySnapshot,

        @Schema(description = "Dias de tolerância para pagamento após vencimento no momento em que o ciclo foi gerado.", example = "15", nullable = true)
        Integer chargeDefinitionLatePaymentGraceDaysSnapshot,

        @Schema(description = "Status do ciclo.", example = "GENERATED")
        ChargeCycleStatus status,

        @Schema(description = "Data em que o ciclo foi gerado.")
        Instant generatedAt,

        @Schema(description = "Data em que o ciclo foi cancelado.", nullable = true)
        Instant canceledAt,

        @Schema(description = "Data em que o ciclo foi finalizado.", nullable = true)
        Instant finishedAt,

        @Schema(description = "Data em que o ciclo foi arquivado.", nullable = true)
        Instant archivedAt
) {

    public static ChargeCycleSummaryResponse from(ChargeCycleResult result) {
        Objects.requireNonNull(result, "result cannot be null");

        return new ChargeCycleSummaryResponse(
                result.id(),
                result.chargeDefinitionId(),
                result.chargeDefinitionNameSnapshot(),
                result.code(),
                result.dueDate(),
                result.chargeDefinitionPaymentAcceptancePolicySnapshot(),
                result.chargeDefinitionLatePaymentGraceDaysSnapshot(),
                result.status(),
                result.generatedAt(),
                result.canceledAt(),
                result.finishedAt(),
                result.archivedAt()
        );
    }
}