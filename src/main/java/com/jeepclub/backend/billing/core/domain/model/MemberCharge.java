package com.jeepclub.backend.billing.core.domain.model;

import com.jeepclub.backend.billing.core.domain.enums.MemberChargeStatus;
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
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = updatedAt;
        this.paidAt = paidAt;
        this.canceledAt = canceledAt;

        validateAmountConsistency(this.originalAmount, this.finalAmount);
        validateStatusConsistency(this.status, this.paidAt, this.canceledAt);
    }

    public static MemberCharge create(
            Long userId,
            Long chargeDefinitionId,
            Long chargeCycleId,
            BigDecimal amount,
            LocalDate dueDate,
            Instant now
    ) {
        Objects.requireNonNull(now, "now cannot be null");

        return new MemberCharge(
                null,
                userId,
                chargeDefinitionId,
                chargeCycleId,
                amount,
                amount,
                dueDate,
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
                status,
                createdAt,
                updatedAt,
                paidAt,
                canceledAt
        );
    }

    public void markAsPaid(Instant paidAt, Instant now) {
        Objects.requireNonNull(paidAt, "paidAt cannot be null");
        Objects.requireNonNull(now, "now cannot be null");

        if (status == MemberChargeStatus.PAID) {
            throw new IllegalStateException("Member charge is already paid.");
        }

        if (status == MemberChargeStatus.CANCELED) {
            throw new IllegalStateException("Canceled member charge cannot be paid.");
        }

        this.status = MemberChargeStatus.PAID;
        this.paidAt = paidAt;
        this.updatedAt = now;
    }

    public void markAsOverdue(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        if (status == MemberChargeStatus.PAID) {
            throw new IllegalStateException("Paid member charge cannot be marked as overdue.");
        }

        if (status == MemberChargeStatus.CANCELED) {
            throw new IllegalStateException("Canceled member charge cannot be marked as overdue.");
        }

        this.status = MemberChargeStatus.OVERDUE;
        this.updatedAt = now;
    }

    public void cancel(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        if (status == MemberChargeStatus.PAID) {
            throw new IllegalStateException("Paid member charge cannot be canceled.");
        }

        if (status == MemberChargeStatus.CANCELED) {
            throw new IllegalStateException("Member charge is already canceled.");
        }

        this.status = MemberChargeStatus.CANCELED;
        this.canceledAt = now;
        this.updatedAt = now;
    }

    public void updateFinalAmount(BigDecimal finalAmount, Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        if (status != MemberChargeStatus.PENDING && status != MemberChargeStatus.OVERDUE) {
            throw new IllegalStateException("Only pending or overdue member charges can have final amount updated.");
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

    public boolean isOverdue() {
        return status == MemberChargeStatus.OVERDUE;
    }

    public boolean isCanceled() {
        return status == MemberChargeStatus.CANCELED;
    }

    public boolean isOpen() {
        return status == MemberChargeStatus.PENDING || status == MemberChargeStatus.OVERDUE;
    }

    public boolean isDue(LocalDate today) {
        Objects.requireNonNull(today, "today cannot be null");

        return isOpen() && today.isAfter(dueDate);
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