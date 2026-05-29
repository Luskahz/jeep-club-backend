package com.jeepclub.backend.billing.core.application.service;

import com.jeepclub.backend.billing.core.application.exception.charge.MemberChargeAccessDeniedException;
import com.jeepclub.backend.billing.core.application.exception.charge.MemberChargeNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.payment.MemberPaymentNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.refund.InvalidRefundPaymentException;
import com.jeepclub.backend.billing.core.application.exception.refund.MemberPaymentAlreadyRefundedException;
import com.jeepclub.backend.billing.core.application.exception.refund.MemberRefundAccessDeniedException;
import com.jeepclub.backend.billing.core.application.exception.refund.MemberRefundNotFoundException;
import com.jeepclub.backend.billing.core.application.result.MemberRefundResult;
import com.jeepclub.backend.billing.core.domain.enums.payment.MemberPaymentStatus;
import com.jeepclub.backend.billing.core.domain.enums.refund.MemberRefundStatus;
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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MemberRefundService {

    private static final Duration REFUND_ELIGIBILITY_DURATION = Duration.ofDays(30);

    private static final List<MemberPaymentStatus> REFUNDABLE_PAYMENT_STATUSES = List.of(
            MemberPaymentStatus.CONFIRMED,
            MemberPaymentStatus.PENDING_VALIDATION
    );

    private final MemberRefundRepository memberRefundRepository;
    private final MemberChargeRepository memberChargeRepository;
    private final MemberPaymentRepository memberPaymentRepository;
    private final Clock clock;

    @Transactional
    public int createEligibilityForCanceledCycle(
            Long chargeCycleId,
            Long createdByUserId,
            Instant canceledAt
    ) {
        Objects.requireNonNull(chargeCycleId, "chargeCycleId cannot be null");
        Objects.requireNonNull(createdByUserId, "createdByUserId cannot be null");
        Objects.requireNonNull(canceledAt, "canceledAt cannot be null");

        Instant eligibleUntil = canceledAt.plus(REFUND_ELIGIBILITY_DURATION);

        List<MemberCharge> memberCharges = memberChargeRepository.findByChargeCycleId(chargeCycleId);

        int createdRefunds = 0;

        for (MemberCharge memberCharge : memberCharges) {
            List<MemberPayment> refundablePayments = memberPaymentRepository.findByMemberChargeIdAndStatusIn(
                    memberCharge.getId(),
                    REFUNDABLE_PAYMENT_STATUSES
            );

            for (MemberPayment memberPayment : refundablePayments) {
                if (memberRefundRepository.existsActiveByMemberPaymentId(memberPayment.getId())) {
                    continue;
                }

                if (memberRefundRepository.existsRefundedByMemberPaymentId(memberPayment.getId())) {
                    continue;
                }

                MemberRefund memberRefund = MemberRefund.createEligibilityForCanceledCycle(
                        memberCharge.getId(),
                        memberPayment.getId(),
                        memberCharge.getChargeCycleId(),
                        memberCharge.getUserId(),
                        memberPayment.getAmount(),
                        createdByUserId,
                        canceledAt,
                        eligibleUntil,
                        canceledAt
                );

                memberRefundRepository.save(memberRefund);
                createdRefunds++;
            }
        }

        return createdRefunds;
    }

    @Transactional
    public MemberRefundResult requestByMemberPaymentId(
            Long authenticatedUserId,
            Long memberPaymentId
    ) {
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId cannot be null");
        Objects.requireNonNull(memberPaymentId, "memberPaymentId cannot be null");

        MemberPayment memberPayment = findMemberPaymentOrThrow(memberPaymentId);

        ensurePaymentCanBeRefunded(memberPayment);

        MemberCharge memberCharge = findMemberChargeOrThrow(memberPayment.getMemberChargeId());

        ensureChargeBelongsToUser(
                memberCharge,
                authenticatedUserId
        );

        ensurePaymentWasNotAlreadyRefunded(memberPayment.getId());

        return memberRefundRepository.findActiveByMemberPaymentId(memberPayment.getId())
                .map(existingRefund -> requestExistingRefundIfEligible(
                        existingRefund,
                        authenticatedUserId
                ))
                .orElseGet(() -> createMemberRequestedRefund(
                        memberCharge,
                        memberPayment,
                        authenticatedUserId
                ));
    }

    @Transactional(readOnly = true)
    public Page<MemberRefundResult> findAll(
            MemberRefundStatus status,
            Pageable pageable
    ) {
        Objects.requireNonNull(pageable, "pageable cannot be null");

        if (status != null) {
            return memberRefundRepository.findByStatus(status, pageable)
                    .map(MemberRefundResult::from);
        }

        return memberRefundRepository.findAll(pageable)
                .map(MemberRefundResult::from);
    }

    @Transactional(readOnly = true)
    public MemberRefundResult findById(Long id) {
        return MemberRefundResult.from(findMemberRefundOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<MemberRefundResult> findByUserId(
            Long userId,
            Pageable pageable
    ) {
        Objects.requireNonNull(userId, "userId cannot be null");
        Objects.requireNonNull(pageable, "pageable cannot be null");

        return memberRefundRepository.findByUserId(userId, pageable)
                .map(MemberRefundResult::from);
    }

    @Transactional(readOnly = true)
    public Page<MemberRefundResult> findByChargeCycleId(
            Long chargeCycleId,
            Pageable pageable
    ) {
        Objects.requireNonNull(chargeCycleId, "chargeCycleId cannot be null");
        Objects.requireNonNull(pageable, "pageable cannot be null");

        return memberRefundRepository.findByChargeCycleId(chargeCycleId, pageable)
                .map(MemberRefundResult::from);
    }

    @Transactional
    public MemberRefundResult request(
            Long refundId,
            Long requestedByUserId
    ) {
        Objects.requireNonNull(requestedByUserId, "requestedByUserId cannot be null");

        MemberRefund memberRefund = findMemberRefundOrThrow(refundId);

        ensureRefundBelongsToUser(
                memberRefund,
                requestedByUserId
        );

        memberRefund.request(
                requestedByUserId,
                Instant.now(clock)
        );

        MemberRefund savedMemberRefund = memberRefundRepository.save(memberRefund);

        return MemberRefundResult.from(savedMemberRefund);
    }

    @Transactional
    public MemberRefundResult approve(
            Long refundId,
            Long approvedByUserId
    ) {
        Objects.requireNonNull(approvedByUserId, "approvedByUserId cannot be null");

        MemberRefund memberRefund = findMemberRefundOrThrow(refundId);

        memberRefund.approve(
                approvedByUserId,
                Instant.now(clock)
        );

        MemberRefund savedMemberRefund = memberRefundRepository.save(memberRefund);

        return MemberRefundResult.from(savedMemberRefund);
    }

    @Transactional
    public MemberRefundResult reject(
            Long refundId,
            Long rejectedByUserId,
            String rejectionReason
    ) {
        Objects.requireNonNull(rejectedByUserId, "rejectedByUserId cannot be null");

        MemberRefund memberRefund = findMemberRefundOrThrow(refundId);

        memberRefund.reject(
                rejectedByUserId,
                rejectionReason,
                Instant.now(clock)
        );

        MemberRefund savedMemberRefund = memberRefundRepository.save(memberRefund);

        return MemberRefundResult.from(savedMemberRefund);
    }

    @Transactional
    public MemberRefundResult markAsRefunded(
            Long refundId,
            Long refundedByUserId
    ) {
        Objects.requireNonNull(refundedByUserId, "refundedByUserId cannot be null");

        MemberRefund memberRefund = findMemberRefundOrThrow(refundId);

        memberRefund.markAsRefunded(
                refundedByUserId,
                Instant.now(clock)
        );

        MemberRefund savedMemberRefund = memberRefundRepository.save(memberRefund);

        return MemberRefundResult.from(savedMemberRefund);
    }

    @Transactional
    public MemberRefundResult expire(Long refundId) {
        MemberRefund memberRefund = findMemberRefundOrThrow(refundId);

        memberRefund.expire(Instant.now(clock));

        MemberRefund savedMemberRefund = memberRefundRepository.save(memberRefund);

        return MemberRefundResult.from(savedMemberRefund);
    }

    @Transactional
    public MemberRefundResult cancel(
            Long refundId,
            Long canceledByUserId
    ) {
        Objects.requireNonNull(canceledByUserId, "canceledByUserId cannot be null");

        MemberRefund memberRefund = findMemberRefundOrThrow(refundId);

        memberRefund.cancel(
                canceledByUserId,
                Instant.now(clock)
        );

        MemberRefund savedMemberRefund = memberRefundRepository.save(memberRefund);

        return MemberRefundResult.from(savedMemberRefund);
    }

    private MemberRefundResult requestExistingRefundIfEligible(
            MemberRefund memberRefund,
            Long requestedByUserId
    ) {
        ensureRefundBelongsToUser(
                memberRefund,
                requestedByUserId
        );

        if (!memberRefund.isEligible()) {
            return MemberRefundResult.from(memberRefund);
        }

        memberRefund.request(
                requestedByUserId,
                Instant.now(clock)
        );

        MemberRefund savedMemberRefund = memberRefundRepository.save(memberRefund);

        return MemberRefundResult.from(savedMemberRefund);
    }

    private MemberRefundResult createMemberRequestedRefund(
            MemberCharge memberCharge,
            MemberPayment memberPayment,
            Long requestedByUserId
    ) {
        MemberRefund memberRefund = MemberRefund.createMemberRequest(
                memberCharge.getId(),
                memberPayment.getId(),
                memberCharge.getChargeCycleId(),
                memberCharge.getUserId(),
                memberPayment.getAmount(),
                requestedByUserId,
                Instant.now(clock)
        );

        MemberRefund savedMemberRefund = memberRefundRepository.save(memberRefund);

        return MemberRefundResult.from(savedMemberRefund);
    }

    private MemberRefund findMemberRefundOrThrow(Long id) {
        Objects.requireNonNull(id, "id cannot be null");

        return memberRefundRepository.findById(id)
                .orElseThrow(() -> new MemberRefundNotFoundException(
                        "Member refund not found."
                ));
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

    private void ensurePaymentWasNotAlreadyRefunded(Long memberPaymentId) {
        Objects.requireNonNull(memberPaymentId, "memberPaymentId cannot be null");

        if (memberRefundRepository.existsRefundedByMemberPaymentId(memberPaymentId)) {
            throw new MemberPaymentAlreadyRefundedException(
                    "Payment has already been refunded."
            );
        }
    }

    private static void ensurePaymentCanBeRefunded(MemberPayment memberPayment) {
        Objects.requireNonNull(memberPayment, "memberPayment cannot be null");

        if (!memberPayment.isConfirmed() && !memberPayment.isPendingValidation()) {
            throw new InvalidRefundPaymentException(
                    "Only confirmed or pending validation payments can be refunded."
            );
        }
    }

    private static void ensureChargeBelongsToUser(
            MemberCharge memberCharge,
            Long userId
    ) {
        Objects.requireNonNull(memberCharge, "memberCharge cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");

        if (!memberCharge.getUserId().equals(userId)) {
            throw new MemberChargeAccessDeniedException(
                    "Member charge does not belong to authenticated user."
            );
        }
    }

    private static void ensureRefundBelongsToUser(
            MemberRefund memberRefund,
            Long userId
    ) {
        Objects.requireNonNull(memberRefund, "memberRefund cannot be null");
        Objects.requireNonNull(userId, "userId cannot be null");

        if (!memberRefund.getUserId().equals(userId)) {
            throw new MemberRefundAccessDeniedException(
                    "Member refund does not belong to authenticated user."
            );
        }
    }
}