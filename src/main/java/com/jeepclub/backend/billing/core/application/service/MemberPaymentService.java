package com.jeepclub.backend.billing.core.application.service;

import com.jeepclub.backend.billing.core.application.exception.memberCharge.MemberChargeAccessDeniedException;
import com.jeepclub.backend.billing.core.application.exception.memberCharge.MemberChargeNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.memberPayment.InvalidPaymentAmountException;
import com.jeepclub.backend.billing.core.application.exception.memberPayment.MemberPaymentNotFoundException;
import com.jeepclub.backend.billing.core.application.result.MemberPaymentResult;
import com.jeepclub.backend.billing.core.domain.enums.MemberPaymentStatus;
import com.jeepclub.backend.billing.core.domain.enums.PaymentMethod;
import com.jeepclub.backend.billing.core.domain.model.MemberCharge;
import com.jeepclub.backend.billing.core.domain.model.MemberPayment;
import com.jeepclub.backend.billing.core.port.PaymentReceiptFile;
import com.jeepclub.backend.billing.core.port.PaymentReceiptStoragePort;
import com.jeepclub.backend.billing.core.port.StoredPaymentReceipt;
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
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MemberPaymentService {

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

        MemberCharge memberCharge = findMemberChargeOrThrow(memberChargeId);

        if (!memberCharge.getUserId().equals(authenticatedUserId)) {
            throw new MemberChargeAccessDeniedException(
                    "Member charge does not belong to authenticated user."
            );
        }

        if (!memberCharge.isOpen()) {
            throw new IllegalStateException("Only open member charges can receive payment submissions.");
        }

        if (amount.compareTo(memberCharge.getFinalAmount()) != 0) {
            throw new InvalidPaymentAmountException(
                    "Payment amount must be equal to member charge final amount."
            );
        }

        StoredPaymentReceipt storedReceipt = paymentReceiptStoragePort.store(receiptFile);

        Instant now = Instant.now(clock);

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

        MemberPayment memberPayment = findMemberPaymentOrThrow(paymentId);
        MemberCharge memberCharge = findMemberChargeOrThrow(memberPayment.getMemberChargeId());

        if (!memberCharge.isOpen()) {
            throw new IllegalStateException("Only open member charges can be paid.");
        }

        Instant now = Instant.now(clock);

        memberPayment.confirm(confirmedByUserId, now);
        memberCharge.markAsPaid(memberPayment.getPaidAt(), now);

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

        MemberPayment memberPayment = findMemberPaymentOrThrow(paymentId);

        memberPayment.reject(
                rejectedByUserId,
                rejectionReason,
                Instant.now(clock)
        );

        MemberPayment savedMemberPayment = memberPaymentRepository.save(memberPayment);

        return MemberPaymentResult.from(savedMemberPayment);
    }

    private MemberPayment findMemberPaymentOrThrow(Long id) {
        Objects.requireNonNull(id, "id cannot be null");

        return memberPaymentRepository.findById(id)
                .orElseThrow(() -> new MemberPaymentNotFoundException(
                        "Member payment not found."
                ));
    }

    private MemberCharge findMemberChargeOrThrow(Long id) {
        Objects.requireNonNull(id, "id cannot be null");

        return memberChargeRepository.findById(id)
                .orElseThrow(() -> new MemberChargeNotFoundException(
                        "Member charge not found."
                ));
    }
}