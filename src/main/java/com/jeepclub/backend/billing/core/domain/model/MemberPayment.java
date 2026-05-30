package com.jeepclub.backend.billing.core.domain.model;

import com.jeepclub.backend.billing.core.domain.enums.payment.MemberPaymentStatus;
import com.jeepclub.backend.billing.core.domain.enums.payment.PaymentMethod;
import com.jeepclub.backend.billing.core.domain.exception.payment.InvalidMemberPaymentStateException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MemberPayment {

    private Long id;
    private Long memberChargeId;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private MemberPaymentStatus status;
    private Instant paidAt;
    private String receiptStorageKey;
    private String receiptUrl;
    private Instant confirmedAt;
    private Long confirmedByUserId;
    private Instant rejectedAt;
    private Long rejectedByUserId;
    private String rejectionReason;
    private Instant canceledAt;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;

    private MemberPayment(
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
        this.id = id;
        this.memberChargeId = validateId(memberChargeId, "memberChargeId");
        this.amount = validateAmount(amount);
        this.paymentMethod = Objects.requireNonNull(paymentMethod, "paymentMethod cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.paidAt = Objects.requireNonNull(paidAt, "paidAt cannot be null");
        this.receiptStorageKey = validateRequiredText(receiptStorageKey, "receiptStorageKey");
        this.receiptUrl = validateRequiredText(receiptUrl, "receiptUrl");
        this.confirmedAt = confirmedAt;
        this.confirmedByUserId = confirmedByUserId;
        this.rejectedAt = rejectedAt;
        this.rejectedByUserId = rejectedByUserId;
        this.rejectionReason = normalizeNullableText(rejectionReason);
        this.canceledAt = canceledAt;
        this.notes = normalizeNullableText(notes);
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = updatedAt;

        validateStatusConsistency();
    }

    public static MemberPayment submitForValidation(
            Long memberChargeId,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            Instant paidAt,
            String receiptStorageKey,
            String receiptUrl,
            String notes,
            Instant now
    ) {
        Objects.requireNonNull(now, "now cannot be null");

        return new MemberPayment(
                null,
                memberChargeId,
                amount,
                paymentMethod,
                MemberPaymentStatus.PENDING_VALIDATION,
                paidAt,
                receiptStorageKey,
                receiptUrl,
                null,
                null,
                null,
                null,
                null,
                null,
                notes,
                now,
                null
        );
    }

    public static MemberPayment reconstitute(
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
        return new MemberPayment(
                id,
                memberChargeId,
                amount,
                paymentMethod,
                status,
                paidAt,
                receiptStorageKey,
                receiptUrl,
                confirmedAt,
                confirmedByUserId,
                rejectedAt,
                rejectedByUserId,
                rejectionReason,
                canceledAt,
                notes,
                createdAt,
                updatedAt
        );
    }

    public void confirm(Long confirmedByUserId, Instant now) {
        validateId(confirmedByUserId, "confirmedByUserId");
        Objects.requireNonNull(now, "now cannot be null");

        if (status != MemberPaymentStatus.PENDING_VALIDATION) {
            throw new InvalidMemberPaymentStateException(
                    "Only pending validation payments can be confirmed."
            );
        }

        this.status = MemberPaymentStatus.CONFIRMED;
        this.confirmedAt = now;
        this.confirmedByUserId = confirmedByUserId;
        this.updatedAt = now;
    }

    public void reject(
            Long rejectedByUserId,
            String rejectionReason,
            Instant now
    ) {
        validateId(rejectedByUserId, "rejectedByUserId");
        Objects.requireNonNull(now, "now cannot be null");

        if (status != MemberPaymentStatus.PENDING_VALIDATION) {
            throw new InvalidMemberPaymentStateException(
                    "Only pending validation payments can be rejected."
            );
        }

        String normalizedReason = validateRequiredText(rejectionReason, "rejectionReason");

        this.status = MemberPaymentStatus.REJECTED;
        this.rejectedAt = now;
        this.rejectedByUserId = rejectedByUserId;
        this.rejectionReason = normalizedReason;
        this.updatedAt = now;
    }

    public void cancel(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        if (status == MemberPaymentStatus.CONFIRMED) {
            throw new InvalidMemberPaymentStateException("Confirmed payment cannot be canceled.");
        }

        if (status == MemberPaymentStatus.CANCELED) {
            throw new InvalidMemberPaymentStateException("Payment is already canceled.");
        }

        this.status = MemberPaymentStatus.CANCELED;
        this.canceledAt = now;
        this.updatedAt = now;
    }

    public void updateSubmission(
            BigDecimal amount,
            PaymentMethod paymentMethod,
            Instant paidAt,
            String receiptStorageKey,
            String receiptUrl,
            String notes,
            Instant now
    ) {
        Objects.requireNonNull(now, "now cannot be null");

        if (status != MemberPaymentStatus.PENDING_VALIDATION
                && status != MemberPaymentStatus.REJECTED) {
            throw new InvalidMemberPaymentStateException(
                    "Only pending validation or rejected payments can be updated."
            );
        }

        this.amount = validateAmount(amount);
        this.paymentMethod = Objects.requireNonNull(paymentMethod, "paymentMethod cannot be null");
        this.paidAt = Objects.requireNonNull(paidAt, "paidAt cannot be null");
        this.receiptStorageKey = validateRequiredText(receiptStorageKey, "receiptStorageKey");
        this.receiptUrl = validateRequiredText(receiptUrl, "receiptUrl");
        this.notes = normalizeNullableText(notes);

        this.status = MemberPaymentStatus.PENDING_VALIDATION;

        this.rejectedAt = null;
        this.rejectedByUserId = null;
        this.rejectionReason = null;

        this.canceledAt = null;
        this.updatedAt = now;

        validateStatusConsistency();
    }

    public boolean isPendingValidation() {
        return status == MemberPaymentStatus.PENDING_VALIDATION;
    }

    public boolean isConfirmed() {
        return status == MemberPaymentStatus.CONFIRMED;
    }

    public boolean isRejected() {
        return status == MemberPaymentStatus.REJECTED;
    }

    public boolean isCanceled() {
        return status == MemberPaymentStatus.CANCELED;
    }

    private void validateStatusConsistency() {
        if (status == MemberPaymentStatus.CONFIRMED) {
            if (confirmedAt == null) {
                throw new IllegalArgumentException("confirmedAt is required when payment is confirmed.");
            }

            validateId(confirmedByUserId, "confirmedByUserId");
        }

        if (status != MemberPaymentStatus.CONFIRMED) {
            if (confirmedAt != null) {
                throw new IllegalArgumentException("confirmedAt must be null when payment is not confirmed.");
            }

            if (confirmedByUserId != null) {
                throw new IllegalArgumentException("confirmedByUserId must be null when payment is not confirmed.");
            }
        }

        if (status == MemberPaymentStatus.REJECTED) {
            if (rejectedAt == null) {
                throw new IllegalArgumentException("rejectedAt is required when payment is rejected.");
            }

            validateId(rejectedByUserId, "rejectedByUserId");

            if (rejectionReason == null) {
                throw new IllegalArgumentException("rejectionReason is required when payment is rejected.");
            }
        }

        if (status != MemberPaymentStatus.REJECTED) {
            if (rejectedAt != null) {
                throw new IllegalArgumentException("rejectedAt must be null when payment is not rejected.");
            }

            if (rejectedByUserId != null) {
                throw new IllegalArgumentException("rejectedByUserId must be null when payment is not rejected.");
            }

            if (rejectionReason != null) {
                throw new IllegalArgumentException("rejectionReason must be null when payment is not rejected.");
            }
        }

        if (status == MemberPaymentStatus.CANCELED && canceledAt == null) {
            throw new IllegalArgumentException("canceledAt is required when payment is canceled.");
        }

        if (status != MemberPaymentStatus.CANCELED && canceledAt != null) {
            throw new IllegalArgumentException("canceledAt must be null when payment is not canceled.");
        }
    }

    private static Long validateId(Long id, String fieldName) {
        Objects.requireNonNull(id, fieldName + " cannot be null");

        if (id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero.");
        }

        return id;
    }

    private static BigDecimal validateAmount(BigDecimal amount) {
        Objects.requireNonNull(amount, "amount cannot be null");

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero.");
        }

        return amount;
    }

    private static String validateRequiredText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }

        return value.trim();
    }

    private static String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}