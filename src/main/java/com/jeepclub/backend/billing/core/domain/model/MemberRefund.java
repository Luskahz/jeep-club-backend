package com.jeepclub.backend.billing.core.domain.model;

import com.jeepclub.backend.billing.core.domain.enums.refund.MemberRefundStatus;
import com.jeepclub.backend.billing.core.domain.enums.refund.RefundReason;

import java.math.BigDecimal;
import java.time.Instant;

public class MemberRefund {

    private Long id;
    private Long memberChargeId;
    private Long memberPaymentId;
    private Long chargeCycleId;
    private Long userId;

    private BigDecimal amount;

    private RefundReason reason;
    private MemberRefundStatus status;

    private Instant eligibleAt;
    private Instant eligibleUntil;

    private Long createdByUserId;

    private Instant requestedAt;
    private Long requestedByUserId;

    private Instant approvedAt;
    private Long approvedByUserId;

    private Instant rejectedAt;
    private Long rejectedByUserId;
    private String rejectionReason;

    private Instant refundedAt;
    private Long refundedByUserId;

    private Instant createdAt;
    private Instant updatedAt;
}
