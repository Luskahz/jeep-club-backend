package com.jeepclub.backend.billing.core.application.service.memberrefund;

import com.jeepclub.backend.billing.core.application.exception.charge.MemberChargeAccessDeniedException;
import com.jeepclub.backend.billing.core.application.exception.charge.MemberChargeNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.payment.MemberPaymentNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.refund.InvalidRefundPaymentException;
import com.jeepclub.backend.billing.core.application.exception.refund.MemberPaymentAlreadyRefundedException;
import com.jeepclub.backend.billing.core.application.exception.refund.MemberRefundAccessDeniedException;
import com.jeepclub.backend.billing.core.application.exception.refund.MemberRefundNotFoundException;
import com.jeepclub.backend.billing.core.application.result.MemberRefundResult;
import com.jeepclub.backend.billing.core.domain.model.MemberCharge;
import com.jeepclub.backend.billing.core.domain.model.MemberPayment;
import com.jeepclub.backend.billing.core.domain.model.MemberRefund;
import com.jeepclub.backend.billing.core.repository.MemberChargeRepository;
import com.jeepclub.backend.billing.core.repository.MemberPaymentRepository;
import com.jeepclub.backend.billing.core.repository.MemberRefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MemberRefundService {

    private final MemberRefundRepository memberRefundRepository;
    private final MemberChargeRepository memberChargeRepository;
    private final MemberPaymentRepository memberPaymentRepository;
    private final Clock clock;

    @Transactional
    public MemberRefundResult requestByMemberPaymentId(Long userId, Long paymentId) {
        Objects.requireNonNull(userId, "authenticatedUserId cannot be null");
        Objects.requireNonNull(paymentId, "memberPaymentId cannot be null");
        MemberPayment payment = findMemberPaymentOrThrow(paymentId);
        ensurePaymentCanBeRefunded(payment);
        MemberCharge charge = findMemberChargeOrThrow(payment.getMemberChargeId());
        ensureChargeBelongsToUser(charge, userId);
        ensurePaymentWasNotAlreadyRefunded(payment.getId());

        return memberRefundRepository.findActiveByMemberPaymentId(payment.getId())
                .map(refund -> requestExistingRefundIfEligible(refund, userId))
                .orElseGet(() -> createMemberRequestedRefund(charge, payment, userId));
    }

    @Transactional(readOnly = true)
    public Page<MemberRefundResult> findByUserId(Long userId, Pageable pageable) {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(pageable, "pageable cannot be null");
        return memberRefundRepository.findByUserId(userId, pageable).map(MemberRefundResult::from);
    }

    @Transactional
    public MemberRefundResult request(Long refundId, Long userId) {
        Objects.requireNonNull(userId, "requestedByUserId cannot be null");
        MemberRefund refund = findMemberRefundOrThrow(refundId);
        ensureRefundBelongsToUser(refund, userId);
        refund.request(userId, Instant.now(clock));
        return MemberRefundResult.from(memberRefundRepository.save(refund));
    }

    private MemberRefundResult requestExistingRefundIfEligible(MemberRefund refund, Long userId) {
        ensureRefundBelongsToUser(refund, userId);
        if (!refund.isEligible()) {
            return MemberRefundResult.from(refund);
        }
        refund.request(userId, Instant.now(clock));
        return MemberRefundResult.from(memberRefundRepository.save(refund));
    }

    private MemberRefundResult createMemberRequestedRefund(
            MemberCharge charge,
            MemberPayment payment,
            Long userId
    ) {
        MemberRefund refund = MemberRefund.createMemberRequest(
                charge.getId(),
                payment.getId(),
                charge.getChargeCycleId(),
                charge.getUserId(),
                payment.getAmount(),
                userId,
                Instant.now(clock)
        );
        return MemberRefundResult.from(memberRefundRepository.save(refund));
    }

    private MemberRefund findMemberRefundOrThrow(Long id) {
        Objects.requireNonNull(id, "id cannot be null");
        return memberRefundRepository.findById(id)
                .orElseThrow(() -> new MemberRefundNotFoundException("Member refund not found."));
    }

    private MemberPayment findMemberPaymentOrThrow(Long id) {
        Objects.requireNonNull(id, "id cannot be null");
        return memberPaymentRepository.findById(id)
                .orElseThrow(() -> new MemberPaymentNotFoundException("Member payment not found."));
    }

    private MemberCharge findMemberChargeOrThrow(Long id) {
        Objects.requireNonNull(id, "id cannot be null");
        return memberChargeRepository.findById(id)
                .orElseThrow(() -> new MemberChargeNotFoundException("Member charge not found."));
    }

    private void ensurePaymentWasNotAlreadyRefunded(Long paymentId) {
        Objects.requireNonNull(paymentId, "memberPaymentId cannot be null");
        if (memberRefundRepository.existsRefundedByMemberPaymentId(paymentId)) {
            throw new MemberPaymentAlreadyRefundedException("Payment has already been refunded.");
        }
    }

    private static void ensurePaymentCanBeRefunded(MemberPayment payment) {
        Objects.requireNonNull(payment, "memberPayment cannot be null");
        if (!payment.isConfirmed() && !payment.isPendingValidation()) {
            throw new InvalidRefundPaymentException(
                    "Only confirmed or pending validation payments can be refunded."
            );
        }
    }

    private static void ensureChargeBelongsToUser(MemberCharge charge, Long userId) {
        Objects.requireNonNull(charge, "memberCharge cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        if (!charge.getUserId().equals(userId)) {
            throw new MemberChargeAccessDeniedException(
                    "Member charge does not belong to authenticated user."
            );
        }
    }

    private static void ensureRefundBelongsToUser(MemberRefund refund, Long userId) {
        Objects.requireNonNull(refund, "memberRefund cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");
        if (!refund.getUserId().equals(userId)) {
            throw new MemberRefundAccessDeniedException(
                    "Member refund does not belong to authenticated user."
            );
        }
    }
}
