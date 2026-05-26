package com.jeepclub.backend.billing.api.dto;

import com.jeepclub.backend.billing.core.application.result.MemberChargeResult;
import com.jeepclub.backend.billing.core.domain.enums.MemberChargeStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Schema(description = "Resposta com os dados completos de uma cobrança de membro.")
public record MemberChargeResponse(

        @Schema(description = "Identificador da cobrança.", example = "1")
        Long id,

        @Schema(description = "Identificador do usuário cobrado.", example = "10")
        Long userId,

        @Schema(description = "Identificador da definição de cobrança.", example = "3")
        Long chargeDefinitionId,

        @Schema(description = "Identificador do ciclo de cobrança.", example = "7")
        Long chargeCycleId,

        @Schema(description = "Valor original da cobrança.", example = "250.00")
        BigDecimal originalAmount,

        @Schema(description = "Valor final da cobrança após ajustes.", example = "200.00")
        BigDecimal finalAmount,

        @Schema(description = "Data de vencimento da cobrança.", example = "2026-02-10")
        LocalDate dueDate,

        @Schema(description = "Status da cobrança.", example = "PENDING")
        MemberChargeStatus status,

        @Schema(description = "Data de criação da cobrança.")
        Instant createdAt,

        @Schema(description = "Data da última atualização da cobrança.", nullable = true)
        Instant updatedAt,

        @Schema(description = "Data em que a cobrança foi paga.", nullable = true)
        Instant paidAt,

        @Schema(description = "Data em que a cobrança foi cancelada.", nullable = true)
        Instant canceledAt
) {

    public static MemberChargeResponse from(MemberChargeResult result) {
        Objects.requireNonNull(result, "result cannot be null");

        return new MemberChargeResponse(
                result.id(),
                result.userId(),
                result.chargeDefinitionId(),
                result.chargeCycleId(),
                result.originalAmount(),
                result.finalAmount(),
                result.dueDate(),
                result.status(),
                result.createdAt(),
                result.updatedAt(),
                result.paidAt(),
                result.canceledAt()
        );
    }
}