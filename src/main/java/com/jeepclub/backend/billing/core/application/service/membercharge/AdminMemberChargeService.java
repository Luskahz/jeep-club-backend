package com.jeepclub.backend.billing.core.application.service.membercharge;

import com.jeepclub.backend.billing.core.application.exception.charge.MemberChargeCannotUpdateFinalAmountException;
import com.jeepclub.backend.billing.core.application.exception.charge.MemberChargeNotFoundException;
import com.jeepclub.backend.billing.core.application.result.charge.MemberChargeResult;
import com.jeepclub.backend.billing.core.domain.enums.charge.MemberChargeStatus;
import com.jeepclub.backend.billing.core.domain.enums.payment.MemberPaymentStatus;
import com.jeepclub.backend.billing.core.domain.model.MemberCharge;
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
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AdminMemberChargeService {

    private final MemberChargeRepository memberChargeRepository;
    private final MemberPaymentRepository memberPaymentRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<MemberChargeResult> findAll(
            Long userId,
            MemberChargeStatus status,
            Pageable pageable
    ) {
        Objects.requireNonNull(pageable, "pageable cannot be null");

        if (userId != null && status != null) {
            return memberChargeRepository.findByUserIdAndStatus(userId, status, pageable)
                    .map(this::toResult);
        }
        if (userId != null) {
            return memberChargeRepository.findByUserId(userId, pageable)
                    .map(this::toResult);
        }
        if (status != null) {
            return memberChargeRepository.findByStatus(status, pageable)
                    .map(this::toResult);
        }
        return memberChargeRepository.findAll(pageable)
                .map(this::toResult);
    }

    @Transactional(readOnly = true)
    public MemberChargeResult findById(Long id) {
        return toResult(findMemberChargeOrThrow(id));
    }

    @Transactional
    public MemberChargeResult updateFinalAmount(Long id, BigDecimal finalAmount) {
        LocalDate today = LocalDate.now(clock);
        Instant now = Instant.now(clock);
        MemberCharge memberCharge = findMemberChargeForUpdateOrThrow(id);

        ensureMemberChargeHasNoPendingValidationPayments(memberCharge.getId());
        memberCharge.updateFinalAmount(finalAmount, today, now);

        return toResult(memberChargeRepository.save(memberCharge));
    }

    @Transactional
    public MemberChargeResult cancel(Long id) {
        MemberCharge memberCharge = findMemberChargeForUpdateOrThrow(id);
        memberCharge.cancel(Instant.now(clock));
        return toResult(memberChargeRepository.save(memberCharge));
    }

    private void ensureMemberChargeHasNoPendingValidationPayments(Long memberChargeId) {
        Objects.requireNonNull(memberChargeId, "memberChargeId cannot be null");

        if (memberPaymentRepository.existsByMemberChargeIdAndStatus(
                memberChargeId,
                MemberPaymentStatus.PENDING_VALIDATION
        )) {
            throw new MemberChargeCannotUpdateFinalAmountException(
                    "Member charge final amount cannot be updated while there are pending validation payments."
            );
        }
    }

    private MemberChargeResult toResult(MemberCharge memberCharge) {
        return MemberChargeResult.from(memberCharge, LocalDate.now(clock));
    }

    private MemberCharge findMemberChargeOrThrow(Long id) {
        Objects.requireNonNull(id, "id cannot be null");
        return memberChargeRepository.findById(id)
                .orElseThrow(() -> new MemberChargeNotFoundException("Member charge not found."));
    }

    private MemberCharge findMemberChargeForUpdateOrThrow(Long id) {
        Objects.requireNonNull(id, "id cannot be null");
        return memberChargeRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new MemberChargeNotFoundException("Member charge not found."));
    }
}
