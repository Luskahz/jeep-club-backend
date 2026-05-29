package com.jeepclub.backend.billing.core.domain.model;

import com.jeepclub.backend.billing.core.domain.enums.refund.MemberRefundStatus;
import com.jeepclub.backend.billing.core.domain.enums.refund.RefundReason;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
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

    private Instant canceledAt;
    private Long canceledByUserId;

    private Instant createdAt;
    private Instant updatedAt;

    private MemberRefund(
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
        this.id = id;
        this.memberChargeId = validateId(memberChargeId, "memberChargeId");
        this.memberPaymentId = validateId(memberPaymentId, "memberPaymentId");
        this.chargeCycleId = validateId(chargeCycleId, "chargeCycleId");
        this.userId = validateId(userId, "userId");
        this.amount = validateAmount(amount);
        this.reason = Objects.requireNonNull(reason, "reason cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.eligibleAt = eligibleAt;
        this.eligibleUntil = eligibleUntil;
        this.createdByUserId = createdByUserId;
        this.requestedAt = requestedAt;
        this.requestedByUserId = requestedByUserId;
        this.approvedAt = approvedAt;
        this.approvedByUserId = approvedByUserId;
        this.rejectedAt = rejectedAt;
        this.rejectedByUserId = rejectedByUserId;
        this.rejectionReason = normalizeNullableText(rejectionReason);
        this.refundedAt = refundedAt;
        this.refundedByUserId = refundedByUserId;
        this.canceledAt = canceledAt;
        this.canceledByUserId = canceledByUserId;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = updatedAt;

        validateStatusConsistency();
    }

    public static MemberRefund createEligibilityForCanceledCycle(
            Long memberChargeId,
            Long memberPaymentId,
            Long chargeCycleId,
            Long userId,
            BigDecimal amount,
            Long createdByUserId,
            Instant eligibleAt,
            Instant eligibleUntil,
            Instant now
    ) {
        validateId(createdByUserId, "createdByUserId");
        Objects.requireNonNull(eligibleAt, "eligibleAt cannot be null");
        Objects.requireNonNull(eligibleUntil, "eligibleUntil cannot be null");
        Objects.requireNonNull(now, "now cannot be null");

        return new MemberRefund(
                null,
                memberChargeId,
                memberPaymentId,
                chargeCycleId,
                userId,
                amount,
                RefundReason.CYCLE_CANCELED_BY_ADMIN,
                MemberRefundStatus.ELIGIBLE,
                eligibleAt,
                eligibleUntil,
                createdByUserId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                now,
                null
        );
    }

    public static MemberRefund createMemberRequest(
            Long memberChargeId,
            Long memberPaymentId,
            Long chargeCycleId,
            Long userId,
            BigDecimal amount,
            Long requestedByUserId,
            Instant now
    ) {
        validateId(requestedByUserId, "requestedByUserId");
        Objects.requireNonNull(now, "now cannot be null");

        return new MemberRefund(
                null,
                memberChargeId,
                memberPaymentId,
                chargeCycleId,
                userId,
                amount,
                RefundReason.MEMBER_REQUEST,
                MemberRefundStatus.REQUESTED,
                null,
                null,
                null,
                now,
                requestedByUserId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                now,
                null
        );
    }

    public static MemberRefund reconstitute(
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
        return new MemberRefund(
                id,
                memberChargeId,
                memberPaymentId,
                chargeCycleId,
                userId,
                amount,
                reason,
                status,
                eligibleAt,
                eligibleUntil,
                createdByUserId,
                requestedAt,
                requestedByUserId,
                approvedAt,
                approvedByUserId,
                rejectedAt,
                rejectedByUserId,
                rejectionReason,
                refundedAt,
                refundedByUserId,
                canceledAt,
                canceledByUserId,
                createdAt,
                updatedAt
        );
    }

    public void request(
            Long requestedByUserId,
            Instant now
    ) {
        validateId(requestedByUserId, "requestedByUserId");
        Objects.requireNonNull(now, "now cannot be null");

        if (status != MemberRefundStatus.ELIGIBLE) {
            throw new IllegalStateException("Only eligible refunds can be requested.");
        }

        if (isEligibilityExpiredAt(now)) {
            throw new IllegalStateException("Expired refund eligibility cannot be requested.");
        }

        this.status = MemberRefundStatus.REQUESTED;
        this.requestedAt = now;
        this.requestedByUserId = requestedByUserId;
        this.updatedAt = now;
    }

    public void approve(
            Long approvedByUserId,
            Instant now
    ) {
        validateId(approvedByUserId, "approvedByUserId");
        Objects.requireNonNull(now, "now cannot be null");

        if (status != MemberRefundStatus.ELIGIBLE && status != MemberRefundStatus.REQUESTED) {
            throw new IllegalStateException("Only eligible or requested refunds can be approved.");
        }

        if (status == MemberRefundStatus.ELIGIBLE && isEligibilityExpiredAt(now)) {
            throw new IllegalStateException("Expired refund eligibility cannot be approved.");
        }

        this.status = MemberRefundStatus.APPROVED;
        this.approvedAt = now;
        this.approvedByUserId = approvedByUserId;
        this.updatedAt = now;
    }

    public void reject(
            Long rejectedByUserId,
            String rejectionReason,
            Instant now
    ) {
        validateId(rejectedByUserId, "rejectedByUserId");
        Objects.requireNonNull(now, "now cannot be null");

        if (status != MemberRefundStatus.REQUESTED) {
            throw new IllegalStateException("Only requested refunds can be rejected.");
        }

        String normalizedReason = validateRequiredText(rejectionReason, "rejectionReason");

        this.status = MemberRefundStatus.REJECTED;
        this.rejectedAt = now;
        this.rejectedByUserId = rejectedByUserId;
        this.rejectionReason = normalizedReason;
        this.updatedAt = now;
    }

    public void markAsRefunded(
            Long refundedByUserId,
            Instant now
    ) {
        validateId(refundedByUserId, "refundedByUserId");
        Objects.requireNonNull(now, "now cannot be null");

        if (status != MemberRefundStatus.APPROVED) {
            throw new IllegalStateException("Only approved refunds can be marked as refunded.");
        }

        this.status = MemberRefundStatus.REFUNDED;
        this.refundedAt = now;
        this.refundedByUserId = refundedByUserId;
        this.updatedAt = now;
    }

    public void expire(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        if (status != MemberRefundStatus.ELIGIBLE) {
            throw new IllegalStateException("Only eligible refunds can expire.");
        }

        if (!isEligibilityExpiredAt(now)) {
            throw new IllegalStateException("Refund eligibility has not expired yet.");
        }

        this.status = MemberRefundStatus.EXPIRED;
        this.updatedAt = now;
    }

    public void cancel(
            Long canceledByUserId,
            Instant now
    ) {
        validateId(canceledByUserId, "canceledByUserId");
        Objects.requireNonNull(now, "now cannot be null");

        if (!isActive()) {
            throw new IllegalStateException("Only active refunds can be canceled.");
        }

        this.status = MemberRefundStatus.CANCELED;
        this.canceledAt = now;
        this.canceledByUserId = canceledByUserId;
        this.updatedAt = now;
    }

    public boolean isEligible() {
        return status == MemberRefundStatus.ELIGIBLE;
    }

    public boolean isRequested() {
        return status == MemberRefundStatus.REQUESTED;
    }

    public boolean isApproved() {
        return status == MemberRefundStatus.APPROVED;
    }

    public boolean isRefunded() {
        return status == MemberRefundStatus.REFUNDED;
    }

    public boolean isExpired() {
        return status == MemberRefundStatus.EXPIRED;
    }

    public boolean isCanceled() {
        return status == MemberRefundStatus.CANCELED;
    }

    public boolean isActive() {
        return status == MemberRefundStatus.ELIGIBLE
                || status == MemberRefundStatus.REQUESTED
                || status == MemberRefundStatus.APPROVED;
    }

    public boolean isEligibilityExpiredAt(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        return eligibleUntil != null && !now.isBefore(eligibleUntil);
    }

    private void validateStatusConsistency() {
        validateOptionalId(createdByUserId, "createdByUserId");
        validateOptionalId(requestedByUserId, "requestedByUserId");
        validateOptionalId(approvedByUserId, "approvedByUserId");
        validateOptionalId(rejectedByUserId, "rejectedByUserId");
        validateOptionalId(refundedByUserId, "refundedByUserId");
        validateOptionalId(canceledByUserId, "canceledByUserId");

        validateEligibilityWindow();

        if (reason == RefundReason.CYCLE_CANCELED_BY_ADMIN) {
            requireEligibilityWindow("Cycle canceled refunds must have an eligibility window.");
            validateId(createdByUserId, "createdByUserId");
        }

        if (status == MemberRefundStatus.ELIGIBLE) {
            requireEligibilityWindow("Eligible refunds must have an eligibility window.");
            ensureNoRequestData();
            ensureNoApprovalData();
            ensureNoRejectionData();
            ensureNoRefundedData();
            ensureNoCancellationData();
        }

        if (status == MemberRefundStatus.REQUESTED) {
            requireRequestData();
            ensureNoApprovalData();
            ensureNoRejectionData();
            ensureNoRefundedData();
            ensureNoCancellationData();
        }

        if (status == MemberRefundStatus.APPROVED) {
            requireApprovalData();
            ensureNoRejectionData();
            ensureNoRefundedData();
            ensureNoCancellationData();
        }

        if (status == MemberRefundStatus.REJECTED) {
            requireRejectionData();
            ensureNoApprovalData();
            ensureNoRefundedData();
            ensureNoCancellationData();
        }

        if (status == MemberRefundStatus.REFUNDED) {
            requireApprovalData();
            requireRefundedData();
            ensureNoRejectionData();
            ensureNoCancellationData();
        }

        if (status == MemberRefundStatus.EXPIRED) {
            requireEligibilityWindow("Expired refunds must have an eligibility window.");
            ensureNoRequestData();
            ensureNoApprovalData();
            ensureNoRejectionData();
            ensureNoRefundedData();
            ensureNoCancellationData();
        }

        if (status == MemberRefundStatus.CANCELED) {
            requireCancellationData();
            ensureNoRejectionData();
            ensureNoRefundedData();
        }
    }

    private void validateEligibilityWindow() {
        if (eligibleAt == null && eligibleUntil == null) {
            return;
        }

        if (eligibleAt == null) {
            throw new IllegalArgumentException("eligibleAt is required when eligibleUntil is provided.");
        }

        if (eligibleUntil == null) {
            throw new IllegalArgumentException("eligibleUntil is required when eligibleAt is provided.");
        }

        if (!eligibleUntil.isAfter(eligibleAt)) {
            throw new IllegalArgumentException("eligibleUntil must be after eligibleAt.");
        }
    }

    private void requireEligibilityWindow(String message) {
        if (eligibleAt == null || eligibleUntil == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requireRequestData() {
        if (requestedAt == null) {
            throw new IllegalArgumentException("requestedAt is required when refund is requested.");
        }

        validateId(requestedByUserId, "requestedByUserId");
    }

    private void requireApprovalData() {
        if (approvedAt == null) {
            throw new IllegalArgumentException("approvedAt is required when refund is approved.");
        }

        validateId(approvedByUserId, "approvedByUserId");
    }

    private void requireRejectionData() {
        if (rejectedAt == null) {
            throw new IllegalArgumentException("rejectedAt is required when refund is rejected.");
        }

        validateId(rejectedByUserId, "rejectedByUserId");

        if (rejectionReason == null) {
            throw new IllegalArgumentException("rejectionReason is required when refund is rejected.");
        }
    }

    private void requireRefundedData() {
        if (refundedAt == null) {
            throw new IllegalArgumentException("refundedAt is required when refund is refunded.");
        }

        validateId(refundedByUserId, "refundedByUserId");
    }

    private void requireCancellationData() {
        if (canceledAt == null) {
            throw new IllegalArgumentException("canceledAt is required when refund is canceled.");
        }

        validateId(canceledByUserId, "canceledByUserId");
    }

    private void ensureNoRequestData() {
        if (requestedAt != null || requestedByUserId != null) {
            throw new IllegalArgumentException("Request data must be null for this refund status.");
        }
    }

    private void ensureNoApprovalData() {
        if (approvedAt != null || approvedByUserId != null) {
            throw new IllegalArgumentException("Approval data must be null for this refund status.");
        }
    }

    private void ensureNoRejectionData() {
        if (rejectedAt != null || rejectedByUserId != null || rejectionReason != null) {
            throw new IllegalArgumentException("Rejection data must be null for this refund status.");
        }
    }

    private void ensureNoRefundedData() {
        if (refundedAt != null || refundedByUserId != null) {
            throw new IllegalArgumentException("Refunded data must be null for this refund status.");
        }
    }

    private void ensureNoCancellationData() {
        if (canceledAt != null || canceledByUserId != null) {
            throw new IllegalArgumentException("Cancellation data must be null for this refund status.");
        }
    }

    private static Long validateId(Long id, String fieldName) {
        Objects.requireNonNull(id, fieldName + " cannot be null");

        if (id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero.");
        }

        return id;
    }

    private static void validateOptionalId(Long id, String fieldName) {
        if (id == null) {
            return;
        }

        validateId(id, fieldName);
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