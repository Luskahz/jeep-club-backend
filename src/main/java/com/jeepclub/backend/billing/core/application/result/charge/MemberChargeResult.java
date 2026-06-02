package com.jeepclub.backend.billing.core.application.result.charge;

import com.jeepclub.backend.billing.core.domain.enums.charge.MemberChargeEffectiveStatus;
import com.jeepclub.backend.billing.core.domain.enums.charge.MemberChargeStatus;
import com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy;
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
        MemberChargeEffectiveStatus effectiveStatus,
        Instant createdAt,
        Instant updatedAt,
        Instant paidAt,
        Instant canceledAt
) {

    public static MemberChargeResult from(
            MemberCharge memberCharge,
            LocalDate referenceDate
    ) {
        Objects.requireNonNull(memberCharge, "memberCharge cannot be null");
        Objects.requireNonNull(referenceDate, "referenceDate cannot be null");

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
                memberCharge.effectiveStatusAt(referenceDate),
                memberCharge.getCreatedAt(),
                memberCharge.getUpdatedAt(),
                memberCharge.getPaidAt(),
                memberCharge.getCanceledAt()
        );
    }
}