package com.jeepclub.backend.billing.api.http.dto.payment;

import com.jeepclub.backend.billing.core.application.result.MemberPaymentResult;
import com.jeepclub.backend.billing.core.domain.enums.payment.MemberPaymentStatus;
import com.jeepclub.backend.billing.core.domain.enums.payment.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Schema(description = "Resumo de um pagamento de membro para listagem.")
public record MemberPaymentSummaryResponse(

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

        @Schema(description = "URL para visualização do comprovante.")
        String receiptUrl,

        @Schema(description = "Data de criação do pagamento.")
        Instant createdAt
) {

    public static MemberPaymentSummaryResponse from(MemberPaymentResult result) {
        Objects.requireNonNull(result, "result cannot be null");

        return new MemberPaymentSummaryResponse(
                result.id(),
                result.memberChargeId(),
                result.amount(),
                result.paymentMethod(),
                result.status(),
                result.paidAt(),
                result.receiptUrl(),
                result.createdAt()
        );
    }
}