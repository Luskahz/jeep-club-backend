package com.jeepclub.backend.billing.core.application.service.memberpayment;

import com.jeepclub.backend.billing.core.application.exception.charge.MemberChargeAccessDeniedException;
import com.jeepclub.backend.billing.core.application.exception.charge.MemberChargeNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.payment.InvalidPaymentAmountException;
import com.jeepclub.backend.billing.core.application.exception.payment.MemberPaymentAlreadyExistsException;
import com.jeepclub.backend.billing.core.application.exception.payment.MemberPaymentNotFoundException;
import com.jeepclub.backend.billing.core.application.result.MemberPaymentResult;
import com.jeepclub.backend.billing.core.domain.enums.payment.MemberPaymentStatus;
import com.jeepclub.backend.billing.core.domain.enums.payment.PaymentMethod;
import com.jeepclub.backend.billing.core.domain.exception.payment.InvalidMemberPaymentStateException;
import com.jeepclub.backend.billing.core.domain.model.MemberCharge;
import com.jeepclub.backend.billing.core.domain.model.MemberPayment;
import com.jeepclub.backend.billing.core.port.payment.PaymentReceiptFile;
import com.jeepclub.backend.billing.core.port.payment.PaymentReceiptStoragePort;
import com.jeepclub.backend.billing.core.port.payment.StoredPaymentReceipt;
import com.jeepclub.backend.billing.core.repository.MemberChargeRepository;
import com.jeepclub.backend.billing.core.repository.MemberPaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MemberPaymentService {

    private static final List<MemberPaymentStatus> EDITABLE_PAYMENT_STATUSES = List.of(
            MemberPaymentStatus.PENDING_VALIDATION,
            MemberPaymentStatus.REJECTED
    );

    private final MemberPaymentRepository memberPaymentRepository;
    private final MemberChargeRepository memberChargeRepository;
    private final PaymentReceiptStoragePort paymentReceiptStoragePort;
    private final Clock clock;

    @Transactional
    public MemberPaymentResult submitForValidation(
            Long authenticatedUserId,
            Long memberChargeId,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            Instant paidAt,
            PaymentReceiptFile receiptFile,
            String notes
    ) {
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId cannot be null");
        Objects.requireNonNull(receiptFile, "receiptFile cannot be null");

        LocalDate today = LocalDate.now(clock);
        Instant now = Instant.now(clock);
        MemberCharge memberCharge = findMemberChargeForUpdateOrThrow(memberChargeId);

        ensureChargeBelongsToAuthenticatedUser(memberCharge, authenticatedUserId);
        ensureNoEditablePaymentExistsForCharge(memberCharge.getId());
        ensureChargeCanReceiveNewPaymentSubmission(memberCharge, today);
        ensurePaymentAmountMatchesCharge(amount, memberCharge);

        StoredPaymentReceipt storedReceipt = paymentReceiptStoragePort.store(receiptFile);
        MemberPayment memberPayment = MemberPayment.submitForValidation(
                memberCharge.getId(),
                amount,
                paymentMethod,
                paidAt,
                storedReceipt.storageKey(),
                storedReceipt.url(),
                notes,
                now
        );
        return MemberPaymentResult.from(memberPaymentRepository.save(memberPayment));
    }

    @Transactional
    public MemberPaymentResult updateSubmission(
            Long authenticatedUserId,
            Long paymentId,
            BigDecimal amount,
            PaymentMethod paymentMethod,
            Instant paidAt,
            PaymentReceiptFile receiptFile,
            String notes
    ) {
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId cannot be null");
        Objects.requireNonNull(receiptFile, "receiptFile cannot be null");

        LocalDate today = LocalDate.now(clock);
        Instant now = Instant.now(clock);
        MemberPayment memberPayment = findMemberPaymentForUpdateOrThrow(paymentId);
        MemberCharge memberCharge = findMemberChargeForUpdateOrThrow(memberPayment.getMemberChargeId());

        ensureChargeBelongsToAuthenticatedUser(memberCharge, authenticatedUserId);
        ensureChargeCanHavePaymentUpdated(memberCharge);
        if (memberPayment.isRejected()) {
            ensureChargeCanReceiveNewPaymentSubmission(memberCharge, today);
        }
        ensurePaymentAmountMatchesCharge(amount, memberCharge);

        StoredPaymentReceipt storedReceipt = paymentReceiptStoragePort.store(receiptFile);
        memberPayment.updateSubmission(
                amount,
                paymentMethod,
                paidAt,
                storedReceipt.storageKey(),
                storedReceipt.url(),
                notes,
                now
        );
        return MemberPaymentResult.from(memberPaymentRepository.save(memberPayment));
    }

    private void ensureNoEditablePaymentExistsForCharge(Long memberChargeId) {
        Objects.requireNonNull(memberChargeId, "memberChargeId cannot be null");
        if (!memberPaymentRepository.findByMemberChargeIdAndStatusIn(
                memberChargeId,
                EDITABLE_PAYMENT_STATUSES
        ).isEmpty()) {
            throw new MemberPaymentAlreadyExistsException(
                    "Member charge already has an editable payment. Update the existing payment instead of creating a new one."
            );
        }
    }

    private static void ensureChargeBelongsToAuthenticatedUser(MemberCharge memberCharge, Long userId) {
        Objects.requireNonNull(memberCharge, "memberCharge cannot be null");
        Objects.requireNonNull(userId, "authenticatedUserId cannot be null");
        if (!memberCharge.getUserId().equals(userId)) {
            throw new MemberChargeAccessDeniedException(
                    "Member charge does not belong to authenticated user."
            );
        }
    }

    private static void ensureChargeCanReceiveNewPaymentSubmission(MemberCharge charge, LocalDate date) {
        Objects.requireNonNull(charge, "memberCharge cannot be null");
        Objects.requireNonNull(date, "paymentSubmissionDate cannot be null");
        if (!charge.acceptsPaymentOn(date)) {
            throw new InvalidMemberPaymentStateException(
                    "Member charge does not accept payment submissions at the current date."
            );
        }
    }

    private static void ensureChargeCanHavePaymentUpdated(MemberCharge charge) {
        Objects.requireNonNull(charge, "memberCharge cannot be null");
        if (!charge.isPending()) {
            throw new InvalidMemberPaymentStateException(
                    "Only pending member charges can have payments updated."
            );
        }
    }

    private static void ensurePaymentAmountMatchesCharge(BigDecimal amount, MemberCharge charge) {
        Objects.requireNonNull(amount, "amount cannot be null");
        Objects.requireNonNull(charge, "memberCharge cannot be null");
        if (amount.compareTo(charge.getFinalAmount()) != 0) {
            throw new InvalidPaymentAmountException(
                    "Payment amount must be equal to member charge final amount."
            );
        }
    }

    private MemberPayment findMemberPaymentForUpdateOrThrow(Long id) {
        Objects.requireNonNull(id, "id cannot be null");
        return memberPaymentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new MemberPaymentNotFoundException("Member payment not found."));
    }

    private MemberCharge findMemberChargeForUpdateOrThrow(Long id) {
        Objects.requireNonNull(id, "id cannot be null");
        return memberChargeRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new MemberChargeNotFoundException("Member charge not found."));
    }
}
