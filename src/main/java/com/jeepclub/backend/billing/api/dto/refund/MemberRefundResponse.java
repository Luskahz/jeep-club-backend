package com.jeepclub.backend.billing.api.dto.refund;

import com.jeepclub.backend.billing.core.application.result.MemberRefundResult;
import com.jeepclub.backend.billing.core.domain.enums.refund.MemberRefundStatus;
import com.jeepclub.backend.billing.core.domain.enums.refund.RefundReason;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Schema(description = "Resposta com os dados completos de um reembolso de membro.")
public record MemberRefundResponse(

        @Schema(description = "Identificador do reembolso.", example = "1")
        Long id,

        @Schema(description = "Identificador da cobrança vinculada ao reembolso.", example = "10")
        Long memberChargeId,

        @Schema(description = "Identificador do pagamento vinculado ao reembolso.", example = "20")
        Long memberPaymentId,

        @Schema(description = "Identificador do ciclo de cobrança vinculado ao reembolso.", example = "5")
        Long chargeCycleId,

        @Schema(description = "Identificador do usuário dono do reembolso.", example = "3")
        Long userId,

        @Schema(description = "Valor previsto para reembolso.", example = "250.00")
        BigDecimal amount,

        @Schema(description = "Motivo do reembolso.", example = "CYCLE_CANCELED_BY_ADMIN")
        RefundReason reason,

        @Schema(description = "Status atual do reembolso.", example = "ELIGIBLE")
        MemberRefundStatus status,

        @Schema(description = "Data em que o reembolso se tornou elegível.", nullable = true)
        Instant eligibleAt,

        @Schema(description = "Data limite para aproveitamento da elegibilidade.", nullable = true)
        Instant eligibleUntil,

        @Schema(description = "Usuário que criou a elegibilidade do reembolso.", example = "1", nullable = true)
        Long createdByUserId,

        @Schema(description = "Data em que o membro solicitou o reembolso.", nullable = true)
        Instant requestedAt,

        @Schema(description = "Usuário que solicitou o reembolso.", example = "3", nullable = true)
        Long requestedByUserId,

        @Schema(description = "Data de aprovação do reembolso.", nullable = true)
        Instant approvedAt,

        @Schema(description = "Usuário que aprovou o reembolso.", example = "1", nullable = true)
        Long approvedByUserId,

        @Schema(description = "Data de rejeição do reembolso.", nullable = true)
        Instant rejectedAt,

        @Schema(description = "Usuário que rejeitou o reembolso.", example = "1", nullable = true)
        Long rejectedByUserId,

        @Schema(description = "Motivo da rejeição do reembolso.", nullable = true)
        String rejectionReason,

        @Schema(description = "Data em que o reembolso foi marcado como realizado.", nullable = true)
        Instant refundedAt,

        @Schema(description = "Usuário que marcou o reembolso como realizado.", example = "1", nullable = true)
        Long refundedByUserId,

        @Schema(description = "Data de cancelamento do processo de reembolso.", nullable = true)
        Instant canceledAt,

        @Schema(description = "Usuário que cancelou o processo de reembolso.", example = "1", nullable = true)
        Long canceledByUserId,

        @Schema(description = "Data de criação do registro de reembolso.")
        Instant createdAt,

        @Schema(description = "Data da última atualização do registro de reembolso.", nullable = true)
        Instant updatedAt
) {

    public static MemberRefundResponse from(MemberRefundResult result) {
        Objects.requireNonNull(result, "result cannot be null");

        return new MemberRefundResponse(
                result.id(),
                result.memberChargeId(),
                result.memberPaymentId(),
                result.chargeCycleId(),
                result.userId(),
                result.amount(),
                result.reason(),
                result.status(),
                result.eligibleAt(),
                result.eligibleUntil(),
                result.createdByUserId(),
                result.requestedAt(),
                result.requestedByUserId(),
                result.approvedAt(),
                result.approvedByUserId(),
                result.rejectedAt(),
                result.rejectedByUserId(),
                result.rejectionReason(),
                result.refundedAt(),
                result.refundedByUserId(),
                result.canceledAt(),
                result.canceledByUserId(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}