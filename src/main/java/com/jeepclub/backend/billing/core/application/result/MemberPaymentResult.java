package com.jeepclub.backend.billing.core.application.result;

import com.jeepclub.backend.billing.core.domain.enums.MemberPaymentStatus;
import com.jeepclub.backend.billing.core.domain.enums.PaymentMethod;
import com.jeepclub.backend.billing.core.domain.model.MemberPayment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record MemberPaymentResult(
        Long id,
        Long memberChargeId,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        MemberPaymentStatus status,
        Instant paidAt,
        String receiptStorageKey,
        String receiptUrl,
        Instant confirmedAt,
        Long confirmedByUserId,
        Instant rejectedAt,
        Long rejectedByUserId,
        String rejectionReason,
        Instant canceledAt,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {

    public static MemberPaymentResult from(MemberPayment memberPayment) {
        Objects.requireNonNull(memberPayment, "memberPayment cannot be null");

        return new MemberPaymentResult(
                memberPayment.getId(),
                memberPayment.getMemberChargeId(),
                memberPayment.getAmount(),
                memberPayment.getPaymentMethod(),
                memberPayment.getStatus(),
                memberPayment.getPaidAt(),
                memberPayment.getReceiptStorageKey(),
                memberPayment.getReceiptUrl(),
                memberPayment.getConfirmedAt(),
                memberPayment.getConfirmedByUserId(),
                memberPayment.getRejectedAt(),
                memberPayment.getRejectedByUserId(),
                memberPayment.getRejectionReason(),
                memberPayment.getCanceledAt(),
                memberPayment.getNotes(),
                memberPayment.getCreatedAt(),
                memberPayment.getUpdatedAt()
        );
    }
}