package com.jeepclub.backend.billing.core.application.service;

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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

        ensureChargeBelongsToAuthenticatedUser(
                memberCharge,
                authenticatedUserId
        );

        ensureNoEditablePaymentExistsForCharge(memberCharge.getId());

        ensureChargeCanReceiveNewPaymentSubmission(
                memberCharge,
                today
        );

        ensurePaymentAmountMatchesCharge(
                amount,
                memberCharge
        );

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

        MemberPayment savedMemberPayment = memberPaymentRepository.save(memberPayment);

        return MemberPaymentResult.from(savedMemberPayment);
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

        ensureChargeBelongsToAuthenticatedUser(
                memberCharge,
                authenticatedUserId
        );

        ensureChargeCanHavePaymentUpdated(memberCharge);

        if (memberPayment.isRejected()) {
            ensureChargeCanReceiveNewPaymentSubmission(
                    memberCharge,
                    today
            );
        }

        ensurePaymentAmountMatchesCharge(
                amount,
                memberCharge
        );

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

        MemberPayment savedMemberPayment = memberPaymentRepository.save(memberPayment);

        return MemberPaymentResult.from(savedMemberPayment);
    }

    @Transactional(readOnly = true)
    public Page<MemberPaymentResult> findAll(
            MemberPaymentStatus status,
            Pageable pageable
    ) {
        Objects.requireNonNull(pageable, "pageable cannot be null");

        if (status != null) {
            return memberPaymentRepository.findByStatus(status, pageable)
                    .map(MemberPaymentResult::from);
        }

        return memberPaymentRepository.findAll(pageable)
                .map(MemberPaymentResult::from);
    }

    @Transactional(readOnly = true)
    public MemberPaymentResult findById(Long id) {
        return MemberPaymentResult.from(findMemberPaymentOrThrow(id));
    }

    @Transactional
    public MemberPaymentResult confirm(
            Long paymentId,
            Long confirmedByUserId
    ) {
        Objects.requireNonNull(confirmedByUserId, "confirmedByUserId cannot be null");

        Instant now = Instant.now(clock);

        MemberPayment memberPayment = findMemberPaymentForUpdateOrThrow(paymentId);
        MemberCharge memberCharge = findMemberChargeForUpdateOrThrow(memberPayment.getMemberChargeId());

        ensureChargeCanBeMarkedAsPaid(memberCharge);

        ensurePaymentAmountStillMatchesCharge(
                memberPayment,
                memberCharge
        );

        memberPayment.confirm(confirmedByUserId, now);
        memberCharge.markAsPaid(
                memberPayment.getPaidAt(),
                now
        );

        memberChargeRepository.save(memberCharge);
        MemberPayment savedMemberPayment = memberPaymentRepository.save(memberPayment);

        return MemberPaymentResult.from(savedMemberPayment);
    }

    @Transactional
    public MemberPaymentResult reject(
            Long paymentId,
            Long rejectedByUserId,
            String rejectionReason
    ) {
        Objects.requireNonNull(rejectedByUserId, "rejectedByUserId cannot be null");

        MemberPayment memberPayment = findMemberPaymentForUpdateOrThrow(paymentId);

        memberPayment.reject(
                rejectedByUserId,
                rejectionReason,
                Instant.now(clock)
        );

        MemberPayment savedMemberPayment = memberPaymentRepository.save(memberPayment);

        return MemberPaymentResult.from(savedMemberPayment);
    }

    private void ensureNoEditablePaymentExistsForCharge(Long memberChargeId) {
        Objects.requireNonNull(memberChargeId, "memberChargeId cannot be null");

        List<MemberPayment> existingPayments = memberPaymentRepository.findByMemberChargeIdAndStatusIn(
                memberChargeId,
                EDITABLE_PAYMENT_STATUSES
        );

        if (!existingPayments.isEmpty()) {
            throw new MemberPaymentAlreadyExistsException(
                    "Member charge already has an editable payment. Update the existing payment instead of creating a new one."
            );
        }
    }

    private static void ensureChargeBelongsToAuthenticatedUser(
            MemberCharge memberCharge,
            Long authenticatedUserId
    ) {
        Objects.requireNonNull(memberCharge, "memberCharge cannot be null");
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId cannot be null");

        if (!memberCharge.getUserId().equals(authenticatedUserId)) {
            throw new MemberChargeAccessDeniedException(
                    "Member charge does not belong to authenticated user."
            );
        }
    }

    private static void ensureChargeCanReceiveNewPaymentSubmission(
            MemberCharge memberCharge,
            LocalDate paymentSubmissionDate
    ) {
        Objects.requireNonNull(memberCharge, "memberCharge cannot be null");
        Objects.requireNonNull(paymentSubmissionDate, "paymentSubmissionDate cannot be null");

        if (!memberCharge.acceptsPaymentOn(paymentSubmissionDate)) {
            throw new InvalidMemberPaymentStateException(
                    "Member charge does not accept payment submissions at the current date."
            );
        }
    }

    private static void ensureChargeCanHavePaymentUpdated(MemberCharge memberCharge) {
        Objects.requireNonNull(memberCharge, "memberCharge cannot be null");

        if (!memberCharge.isPending()) {
            throw new InvalidMemberPaymentStateException(
                    "Only pending member charges can have payments updated."
            );
        }
    }

    private static void ensureChargeCanBeMarkedAsPaid(MemberCharge memberCharge) {
        Objects.requireNonNull(memberCharge, "memberCharge cannot be null");

        if (!memberCharge.isPending()) {
            throw new InvalidMemberPaymentStateException(
                    "Only pending member charges can be paid."
            );
        }
    }

    private static void ensurePaymentAmountMatchesCharge(
            BigDecimal amount,
            MemberCharge memberCharge
    ) {
        Objects.requireNonNull(amount, "amount cannot be null");
        Objects.requireNonNull(memberCharge, "memberCharge cannot be null");

        if (amount.compareTo(memberCharge.getFinalAmount()) != 0) {
            throw new InvalidPaymentAmountException(
                    "Payment amount must be equal to member charge final amount."
            );
        }
    }

    private static void ensurePaymentAmountStillMatchesCharge(
            MemberPayment memberPayment,
            MemberCharge memberCharge
    ) {
        Objects.requireNonNull(memberPayment, "memberPayment cannot be null");
        Objects.requireNonNull(memberCharge, "memberCharge cannot be null");

        if (memberPayment.getAmount().compareTo(memberCharge.getFinalAmount()) != 0) {
            throw new InvalidMemberPaymentStateException(
                    "Payment amount no longer matches member charge final amount."
            );
        }
    }

    private MemberPayment findMemberPaymentOrThrow(Long id) {
        Objects.requireNonNull(id, "id cannot be null");

        return memberPaymentRepository.findById(id)
                .orElseThrow(() -> new MemberPaymentNotFoundException(
                        "Member payment not found."
                ));
    }

    private MemberPayment findMemberPaymentForUpdateOrThrow(Long id) {
        Objects.requireNonNull(id, "id cannot be null");

        return memberPaymentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new MemberPaymentNotFoundException(
                        "Member payment not found."
                ));
    }

    private MemberCharge findMemberChargeForUpdateOrThrow(Long id) {
        Objects.requireNonNull(id, "id cannot be null");

        return memberChargeRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new MemberChargeNotFoundException(
                        "Member charge not found."
                ));
    }
}