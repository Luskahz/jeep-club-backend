package com.jeepclub.backend.billing.core.domain.model;

import com.jeepclub.backend.billing.core.domain.enums.charge.MemberChargeEffectiveStatus;
import com.jeepclub.backend.billing.core.domain.enums.charge.MemberChargeStatus;
import com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy;
import com.jeepclub.backend.billing.core.domain.exception.charge.InvalidMemberChargeStateException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberCharge {

    private Long id;
    private Long userId;
    private Long chargeDefinitionId;
    private Long chargeCycleId;
    private BigDecimal originalAmount;
    private BigDecimal finalAmount;
    private LocalDate dueDate;
    private PaymentAcceptancePolicy paymentAcceptancePolicy;
    private Integer latePaymentGraceDays;
    private LocalDate paymentAllowedUntil;
    private MemberChargeStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant paidAt;
    private Instant canceledAt;

    private MemberCharge(
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
            Instant canceledAt
    ) {
        this.id = id;
        this.userId = validateId(userId, "userId");
        this.chargeDefinitionId = validateId(chargeDefinitionId, "chargeDefinitionId");
        this.chargeCycleId = validateId(chargeCycleId, "chargeCycleId");
        this.originalAmount = validateAmount(originalAmount, "originalAmount");
        this.finalAmount = validateAmount(finalAmount, "finalAmount");
        this.dueDate = Objects.requireNonNull(dueDate, "dueDate cannot be null");
        this.paymentAcceptancePolicy = Objects.requireNonNull(
                paymentAcceptancePolicy,
                "paymentAcceptancePolicy cannot be null"
        );
        this.latePaymentGraceDays = latePaymentGraceDays;
        this.paymentAllowedUntil = paymentAllowedUntil;
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = updatedAt;
        this.paidAt = paidAt;
        this.canceledAt = canceledAt;

        validateAmountConsistency(this.originalAmount, this.finalAmount);
        validatePaymentAcceptancePolicyConsistency(
                this.paymentAcceptancePolicy,
                this.latePaymentGraceDays,
                this.dueDate,
                this.paymentAllowedUntil
        );
        validateStatusConsistency(
                this.status,
                this.paidAt,
                this.canceledAt
        );
    }

    public static MemberCharge create(
            Long userId,
            Long chargeDefinitionId,
            Long chargeCycleId,
            BigDecimal amount,
            LocalDate dueDate,
            PaymentAcceptancePolicy paymentAcceptancePolicy,
            Integer latePaymentGraceDays,
            Instant now
    ) {
        Objects.requireNonNull(now, "now cannot be null");

        LocalDate paymentAllowedUntil = calculatePaymentAllowedUntil(
                dueDate,
                paymentAcceptancePolicy,
                latePaymentGraceDays
        );

        return new MemberCharge(
                null,
                userId,
                chargeDefinitionId,
                chargeCycleId,
                amount,
                amount,
                dueDate,
                paymentAcceptancePolicy,
                latePaymentGraceDays,
                paymentAllowedUntil,
                MemberChargeStatus.PENDING,
                now,
                null,
                null,
                null
        );
    }

    public static MemberCharge reconstitute(
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
            Instant canceledAt
    ) {
        return new MemberCharge(
                id,
                userId,
                chargeDefinitionId,
                chargeCycleId,
                originalAmount,
                finalAmount,
                dueDate,
                paymentAcceptancePolicy,
                latePaymentGraceDays,
                paymentAllowedUntil,
                status,
                createdAt,
                updatedAt,
                paidAt,
                canceledAt
        );
    }

    public void markAsPaid(
            Instant paidAt,
            Instant now
    ) {
        Objects.requireNonNull(paidAt, "paidAt cannot be null");
        Objects.requireNonNull(now, "now cannot be null");

        if (status == MemberChargeStatus.PAID) {
            throw new InvalidMemberChargeStateException("Member charge is already paid.");
        }

        if (status == MemberChargeStatus.CANCELED) {
            throw new InvalidMemberChargeStateException("Canceled member charge cannot be paid.");
        }

        this.status = MemberChargeStatus.PAID;
        this.paidAt = paidAt;
        this.updatedAt = now;
    }

    public void cancel(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        if (status == MemberChargeStatus.PAID) {
            throw new InvalidMemberChargeStateException("Paid member charge cannot be canceled.");
        }

        if (status == MemberChargeStatus.CANCELED) {
            throw new InvalidMemberChargeStateException("Member charge is already canceled.");
        }

        this.status = MemberChargeStatus.CANCELED;
        this.canceledAt = now;
        this.updatedAt = now;
    }

    public void updateFinalAmount(
            BigDecimal finalAmount,
            LocalDate referenceDate,
            Instant now
    ) {
        Objects.requireNonNull(referenceDate, "referenceDate cannot be null");
        Objects.requireNonNull(now, "now cannot be null");

        if (status != MemberChargeStatus.PENDING) {
            throw new InvalidMemberChargeStateException(
                    "Only pending member charges can have final amount updated."
            );
        }

        if (isExpiredAt(referenceDate)) {
            throw new InvalidMemberChargeStateException(
                    "Expired member charges cannot have final amount updated."
            );
        }

        BigDecimal validatedFinalAmount = validateAmount(finalAmount, "finalAmount");

        validateAmountConsistency(this.originalAmount, validatedFinalAmount);

        this.finalAmount = validatedFinalAmount;
        this.updatedAt = now;
    }

    public boolean isPaid() {
        return status == MemberChargeStatus.PAID;
    }

    public boolean isPending() {
        return status == MemberChargeStatus.PENDING;
    }

    public boolean isCanceled() {
        return status == MemberChargeStatus.CANCELED;
    }

    public boolean isOpen() {
        return status == MemberChargeStatus.PENDING;
    }

    public boolean isDue(LocalDate today) {
        Objects.requireNonNull(today, "today cannot be null");

        return isOpen() && today.isAfter(dueDate);
    }

    public boolean acceptsPaymentOn(LocalDate paymentDate) {
        Objects.requireNonNull(paymentDate, "paymentDate cannot be null");

        return isOpen() && !isExpiredAt(paymentDate);
    }

    public boolean isOverdueAt(LocalDate today) {
        Objects.requireNonNull(today, "today cannot be null");

        return isOpen()
                && today.isAfter(dueDate)
                && !isExpiredAt(today);
    }

    public boolean isExpiredAt(LocalDate today) {
        Objects.requireNonNull(today, "today cannot be null");

        return isOpen()
                && paymentAllowedUntil != null
                && today.isAfter(paymentAllowedUntil);
    }

    public MemberChargeEffectiveStatus effectiveStatusAt(LocalDate today) {
        Objects.requireNonNull(today, "today cannot be null");

        if (isPaid()) {
            return MemberChargeEffectiveStatus.PAID;
        }

        if (isCanceled()) {
            return MemberChargeEffectiveStatus.CANCELED;
        }

        if (isExpiredAt(today)) {
            return MemberChargeEffectiveStatus.EXPIRED;
        }

        if (isOverdueAt(today)) {
            return MemberChargeEffectiveStatus.OVERDUE;
        }

        return MemberChargeEffectiveStatus.PENDING;
    }

    private static LocalDate calculatePaymentAllowedUntil(
            LocalDate dueDate,
            PaymentAcceptancePolicy paymentAcceptancePolicy,
            Integer latePaymentGraceDays
    ) {
        Objects.requireNonNull(dueDate, "dueDate cannot be null");
        Objects.requireNonNull(paymentAcceptancePolicy, "paymentAcceptancePolicy cannot be null");

        return switch (paymentAcceptancePolicy) {
            case UNTIL_DUE_DATE -> dueDate;
            case AFTER_DUE_DATE -> null;
            case UNTIL_DAYS_AFTER_DUE_DATE -> {
                if (latePaymentGraceDays == null) {
                    throw new IllegalArgumentException(
                            "latePaymentGraceDays is required when paymentAcceptancePolicy is UNTIL_DAYS_AFTER_DUE_DATE."
                    );
                }

                if (latePaymentGraceDays <= 0) {
                    throw new IllegalArgumentException("latePaymentGraceDays must be greater than zero.");
                }

                yield dueDate.plusDays(latePaymentGraceDays);
            }
        };
    }

    private static void validatePaymentAcceptancePolicyConsistency(
            PaymentAcceptancePolicy paymentAcceptancePolicy,
            Integer latePaymentGraceDays,
            LocalDate dueDate,
            LocalDate paymentAllowedUntil
    ) {
        LocalDate expectedPaymentAllowedUntil = calculatePaymentAllowedUntil(
                dueDate,
                paymentAcceptancePolicy,
                latePaymentGraceDays
        );

        if (!Objects.equals(expectedPaymentAllowedUntil, paymentAllowedUntil)) {
            throw new IllegalArgumentException("paymentAllowedUntil is inconsistent with payment acceptance policy.");
        }

        if (paymentAcceptancePolicy != PaymentAcceptancePolicy.UNTIL_DAYS_AFTER_DUE_DATE
                && latePaymentGraceDays != null) {
            throw new IllegalArgumentException(
                    "latePaymentGraceDays must be null when paymentAcceptancePolicy does not use a grace period."
            );
        }
    }

    private static Long validateId(Long id, String fieldName) {
        Objects.requireNonNull(id, fieldName + " cannot be null");

        if (id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero.");
        }

        return id;
    }

    private static BigDecimal validateAmount(BigDecimal amount, String fieldName) {
        Objects.requireNonNull(amount, fieldName + " cannot be null");

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero.");
        }

        return amount;
    }

    private static void validateAmountConsistency(
            BigDecimal originalAmount,
            BigDecimal finalAmount
    ) {
        if (finalAmount.compareTo(originalAmount) > 0) {
            throw new IllegalArgumentException("finalAmount cannot be greater than originalAmount.");
        }
    }

    private static void validateStatusConsistency(
            MemberChargeStatus status,
            Instant paidAt,
            Instant canceledAt
    ) {
        if (status == MemberChargeStatus.PAID && paidAt == null) {
            throw new IllegalArgumentException("paidAt is required when member charge is paid.");
        }

        if (status != MemberChargeStatus.PAID && paidAt != null) {
            throw new IllegalArgumentException("paidAt must be null when member charge is not paid.");
        }

        if (status == MemberChargeStatus.CANCELED && canceledAt == null) {
            throw new IllegalArgumentException("canceledAt is required when member charge is canceled.");
        }

        if (status != MemberChargeStatus.CANCELED && canceledAt != null) {
            throw new IllegalArgumentException("canceledAt must be null when member charge is not canceled.");
        }
    }
}