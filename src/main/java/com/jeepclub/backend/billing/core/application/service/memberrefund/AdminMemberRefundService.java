package com.jeepclub.backend.billing.core.application.service.memberrefund;

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
public class AdminMemberRefundService {

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
    public int createEligibilityForCanceledCycle(Long cycleId, Long userId, Instant canceledAt) {
        Objects.requireNonNull(cycleId, "chargeCycleId cannot be null");
        Objects.requireNonNull(userId, "createdByUserId cannot be null");
        Objects.requireNonNull(canceledAt, "canceledAt cannot be null");
        Instant eligibleUntil = canceledAt.plus(REFUND_ELIGIBILITY_DURATION);
        int createdRefunds = 0;

        for (MemberCharge charge : memberChargeRepository.findByChargeCycleId(cycleId)) {
            List<MemberPayment> payments = memberPaymentRepository.findByMemberChargeIdAndStatusIn(
                    charge.getId(),
                    REFUNDABLE_PAYMENT_STATUSES
            );
            for (MemberPayment payment : payments) {
                if (memberRefundRepository.existsActiveByMemberPaymentId(payment.getId())
                        || memberRefundRepository.existsRefundedByMemberPaymentId(payment.getId())) {
                    continue;
                }
                MemberRefund refund = MemberRefund.createEligibilityForCanceledCycle(
                        charge.getId(),
                        payment.getId(),
                        charge.getChargeCycleId(),
                        charge.getUserId(),
                        payment.getAmount(),
                        userId,
                        canceledAt,
                        eligibleUntil,
                        canceledAt
                );
                memberRefundRepository.save(refund);
                createdRefunds++;
            }
        }
        return createdRefunds;
    }

    @Transactional(readOnly = true)
    public Page<MemberRefundResult> findAll(MemberRefundStatus status, Pageable pageable) {
        Objects.requireNonNull(pageable, "pageable cannot be null");
        if (status != null) {
            return memberRefundRepository.findByStatus(status, pageable).map(MemberRefundResult::from);
        }
        return memberRefundRepository.findAll(pageable).map(MemberRefundResult::from);
    }

    @Transactional(readOnly = true)
    public MemberRefundResult findById(Long id) {
        return MemberRefundResult.from(findMemberRefundOrThrow(id));
    }

    @Transactional(readOnly = true)
    public Page<MemberRefundResult> findByChargeCycleId(Long cycleId, Pageable pageable) {
        Objects.requireNonNull(cycleId, "chargeCycleId cannot be null");
        Objects.requireNonNull(pageable, "pageable cannot be null");
        return memberRefundRepository.findByChargeCycleId(cycleId, pageable).map(MemberRefundResult::from);
    }

    @Transactional
    public MemberRefundResult approve(Long id, Long userId) {
        Objects.requireNonNull(userId, "approvedByUserId cannot be null");
        MemberRefund refund = findMemberRefundOrThrow(id);
        refund.approve(userId, Instant.now(clock));
        return MemberRefundResult.from(memberRefundRepository.save(refund));
    }

    @Transactional
    public MemberRefundResult reject(Long id, Long userId, String reason) {
        Objects.requireNonNull(userId, "rejectedByUserId cannot be null");
        MemberRefund refund = findMemberRefundOrThrow(id);
        refund.reject(userId, reason, Instant.now(clock));
        return MemberRefundResult.from(memberRefundRepository.save(refund));
    }

    @Transactional
    public MemberRefundResult markAsRefunded(Long id, Long userId) {
        Objects.requireNonNull(userId, "refundedByUserId cannot be null");
        MemberRefund refund = findMemberRefundOrThrow(id);
        refund.markAsRefunded(userId, Instant.now(clock));
        return MemberRefundResult.from(memberRefundRepository.save(refund));
    }

    @Transactional
    public MemberRefundResult expire(Long id) {
        MemberRefund refund = findMemberRefundOrThrow(id);
        refund.expire(Instant.now(clock));
        return MemberRefundResult.from(memberRefundRepository.save(refund));
    }

    @Transactional
    public MemberRefundResult cancel(Long id, Long userId) {
        Objects.requireNonNull(userId, "canceledByUserId cannot be null");
        MemberRefund refund = findMemberRefundOrThrow(id);
        refund.cancel(userId, Instant.now(clock));
        return MemberRefundResult.from(memberRefundRepository.save(refund));
    }

    private MemberRefund findMemberRefundOrThrow(Long id) {
        Objects.requireNonNull(id, "id cannot be null");
        return memberRefundRepository.findById(id)
                .orElseThrow(() -> new MemberRefundNotFoundException("Member refund not found."));
    }
}
