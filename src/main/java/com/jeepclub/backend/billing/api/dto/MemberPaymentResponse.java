package com.jeepclub.backend.billing.api.dto;

import com.jeepclub.backend.billing.core.application.result.MemberPaymentResult;
import com.jeepclub.backend.billing.core.domain.enums.MemberPaymentStatus;
import com.jeepclub.backend.billing.core.domain.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Schema(description = "Resposta com os dados completos de um pagamento de membro.")
public record MemberPaymentResponse(

        @Schema(description = "Identificador do pagamento.", example = "1")
        Long id,

        @Schema(description = "Identificador da cobrança de membro.", example = "10")
        Long memberChargeId,

        @Schema(description = "Valor informado no pagamento.", example = "250.00")
        BigDecimal amount,

        @Schema(description = "Método de pagamento.", example = "PIX")
        PaymentMethod paymentMethod,

        @Schema(description = "Status do pagamento.", example = "PENDING_VALIDATION")
        MemberPaymentStatus status,

        @Schema(description = "Data informada em que o pagamento foi realizado.")
        Instant paidAt,

        @Schema(description = "Chave interna do comprovante armazenado.")
        String receiptStorageKey,

        @Schema(description = "URL para visualização do comprovante.")
        String receiptUrl,

        @Schema(description = "Data de confirmação do pagamento.", nullable = true)
        Instant confirmedAt,

        @Schema(description = "Usuário que confirmou o pagamento.", example = "1", nullable = true)
        Long confirmedByUserId,

        @Schema(description = "Data de rejeição do pagamento.", nullable = true)
        Instant rejectedAt,

        @Schema(description = "Usuário que rejeitou o pagamento.", example = "1", nullable = true)
        Long rejectedByUserId,

        @Schema(description = "Motivo da rejeição.", nullable = true)
        String rejectionReason,

        @Schema(description = "Data de cancelamento do pagamento.", nullable = true)
        Instant canceledAt,

        @Schema(description = "Observações do pagamento.", nullable = true)
        String notes,

        @Schema(description = "Data de criação do pagamento.")
        Instant createdAt,

        @Schema(description = "Data da última atualização.", nullable = true)
        Instant updatedAt
) {

    public static MemberPaymentResponse from(MemberPaymentResult result) {
        Objects.requireNonNull(result, "result cannot be null");

        return new MemberPaymentResponse(
                result.id(),
                result.memberChargeId(),
                result.amount(),
                result.paymentMethod(),
                result.status(),
                result.paidAt(),
                result.receiptStorageKey(),
                result.receiptUrl(),
                result.confirmedAt(),
                result.confirmedByUserId(),
                result.rejectedAt(),
                result.rejectedByUserId(),
                result.rejectionReason(),
                result.canceledAt(),
                result.notes(),
                result.createdAt(),
                result.updatedAt()
        );
    }
}