package com.jeepclub.backend.billing.core.application.service;

import com.jeepclub.backend.billing.core.application.exception.refund.MemberRefundNotFoundException;
import com.jeepclub.backend.billing.core.application.result.MemberRefundResult;
import com.jeepclub.backend.billing.core.domain.enums.MemberPaymentStatus;
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

    private MemberRefund findMemberRefundOrThrow(Long id) {
        Objects.requireNonNull(id, "id cannot be null");

        return memberRefundRepository.findById(id)
                .orElseThrow(() -> new MemberRefundNotFoundException(
                        "Member refund not found."
                ));
    }
}