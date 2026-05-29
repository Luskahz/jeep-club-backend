package com.jeepclub.backend.billing.core.application.result;

import com.jeepclub.backend.billing.core.domain.enums.MemberChargeStatus;
import com.jeepclub.backend.billing.core.domain.enums.PaymentAcceptancePolicy;
import com.jeepclub.backend.billing.core.domain.model.MemberCharge;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public record MemberChargeResult(
        Long id,
        Long userId,
        Long chargeDefinitionId,
        Long chargeCycleId,
        BigDecimal originalAmount,
        BigDecimal finalAmount,
        LocalDate dueDate,
        PaymentAcceptancePolicy paymentAcceptancePolicy,
        Integer latePaymentGraceDays,
        LocalDate paymentAllowedUntil,
        MemberChargeStatus status,
        Instant createdAt,
        Instant updatedAt,
        Instant paidAt,
        Instant canceledAt,
        Instant expiredAt
) {

    public static MemberChargeResult from(MemberCharge memberCharge) {
        Objects.requireNonNull(memberCharge, "memberCharge cannot be null");

        return new MemberChargeResult(
                memberCharge.getId(),
                memberCharge.getUserId(),
                memberCharge.getChargeDefinitionId(),
                memberCharge.getChargeCycleId(),
                memberCharge.getOriginalAmount(),
                memberCharge.getFinalAmount(),
                memberCharge.getDueDate(),
                memberCharge.getPaymentAcceptancePolicy(),
                memberCharge.getLatePaymentGraceDays(),
                memberCharge.getPaymentAllowedUntil(),
                memberCharge.getStatus(),
                memberCharge.getCreatedAt(),
                memberCharge.getUpdatedAt(),
                memberCharge.getPaidAt(),
                memberCharge.getCanceledAt(),
                memberCharge.getExpiredAt()
        );
    }
}