package com.jeepclub.backend.billing.api.dto.charge;

import com.jeepclub.backend.billing.core.application.result.charge.MemberChargeResult;
import com.jeepclub.backend.billing.core.domain.enums.charge.MemberChargeEffectiveStatus;
import com.jeepclub.backend.billing.core.domain.enums.charge.MemberChargeStatus;
import com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

@Schema(description = "Resumo de uma cobrança de membro para listagem.")
public record MemberChargeSummaryResponse(

        @Schema(description = "Identificador da cobrança.", example = "1")
        Long id,

        @Schema(description = "Identificador do usuário cobrado.", example = "10")
        Long userId,

        @Schema(description = "Identificador da definição de cobrança.", example = "3")
        Long chargeDefinitionId,

        @Schema(description = "Identificador do ciclo de cobrança.", example = "7")
        Long chargeCycleId,

        @Schema(description = "Valor final da cobrança.", example = "250.00")
        BigDecimal finalAmount,

        @Schema(description = "Data de vencimento da cobrança.", example = "2026-02-10")
        LocalDate dueDate,

        @Schema(description = "Política de aceitação de pagamento da cobrança.", example = "AFTER_DUE_DATE")
        PaymentAcceptancePolicy paymentAcceptancePolicy,

        @Schema(description = "Última data em que a cobrança aceita pagamento. Nulo significa sem limite definido.", example = "2026-02-25", nullable = true)
        LocalDate paymentAllowedUntil,

        @Schema(description = "Status persistido da cobrança.", example = "PENDING")
        MemberChargeStatus status,

        @Schema(description = "Status calculado da cobrança na data da consulta.", example = "OVERDUE")
        MemberChargeEffectiveStatus effectiveStatus
) {

    public static MemberChargeSummaryResponse from(MemberChargeResult result) {
        Objects.requireNonNull(result, "result cannot be null");

        return new MemberChargeSummaryResponse(
                result.id(),
                result.userId(),
                result.chargeDefinitionId(),
                result.chargeCycleId(),
                result.finalAmount(),
                result.dueDate(),
                result.paymentAcceptancePolicy(),
                result.paymentAllowedUntil(),
                result.status(),
                result.effectiveStatus()
        );
    }
}