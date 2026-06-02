package com.jeepclub.backend.billing.core.application.result;

import com.jeepclub.backend.billing.core.domain.enums.refund.MemberRefundStatus;
import com.jeepclub.backend.billing.core.domain.enums.refund.RefundReason;
import com.jeepclub.backend.billing.core.domain.model.MemberRefund;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record MemberRefundResult(
        Long id,
        Long memberChargeId,
        Long memberPaymentId,
        Long chargeCycleId,
        Long userId,
        BigDecimal amount,
        RefundReason reason,
        MemberRefundStatus status,
        Instant eligibleAt,
        Instant eligibleUntil,
        Long createdByUserId,
        Instant requestedAt,
        Long requestedByUserId,
        Instant approvedAt,
        Long approvedByUserId,
        Instant rejectedAt,
        Long rejectedByUserId,
        String rejectionReason,
        Instant refundedAt,
        Long refundedByUserId,
        Instant canceledAt,
        Long canceledByUserId,
        Instant createdAt,
        Instant updatedAt
) {

    public static MemberRefundResult from(MemberRefund memberRefund) {
        Objects.requireNonNull(memberRefund, "memberRefund cannot be null");

        return new MemberRefundResult(
                memberRefund.getId(),
                memberRefund.getMemberChargeId(),
                memberRefund.getMemberPaymentId(),
                memberRefund.getChargeCycleId(),
                memberRefund.getUserId(),
                memberRefund.getAmount(),
                memberRefund.getReason(),
                memberRefund.getStatus(),
                memberRefund.getEligibleAt(),
                memberRefund.getEligibleUntil(),
                memberRefund.getCreatedByUserId(),
                memberRefund.getRequestedAt(),
                memberRefund.getRequestedByUserId(),
                memberRefund.getApprovedAt(),
                memberRefund.getApprovedByUserId(),
                memberRefund.getRejectedAt(),
                memberRefund.getRejectedByUserId(),
                memberRefund.getRejectionReason(),
                memberRefund.getRefundedAt(),
                memberRefund.getRefundedByUserId(),
                memberRefund.getCanceledAt(),
                memberRefund.getCanceledByUserId(),
                memberRefund.getCreatedAt(),
                memberRefund.getUpdatedAt()
        );
    }
}