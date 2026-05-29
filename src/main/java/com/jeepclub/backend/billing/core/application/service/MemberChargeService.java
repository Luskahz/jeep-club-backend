package com.jeepclub.backend.billing.core.application.service;

import com.jeepclub.backend.billing.core.application.exception.charge.MemberChargeAccessDeniedException;
import com.jeepclub.backend.billing.core.application.exception.charge.MemberChargeNotFoundException;
import com.jeepclub.backend.billing.core.application.result.MemberChargeResult;
import com.jeepclub.backend.billing.core.application.result.RefreshMemberChargeStatusesResult;
import com.jeepclub.backend.billing.core.domain.enums.MemberChargeStatus;
import com.jeepclub.backend.billing.core.domain.model.MemberCharge;
import com.jeepclub.backend.billing.core.repository.MemberChargeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MemberChargeService {

    private final MemberChargeRepository memberChargeRepository;
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
                    .map(MemberChargeResult::from);
        }

        if (userId != null) {
            return memberChargeRepository.findByUserId(userId, pageable)
                    .map(MemberChargeResult::from);
        }

        if (status != null) {
            return memberChargeRepository.findByStatus(status, pageable)
                    .map(MemberChargeResult::from);
        }

        return memberChargeRepository.findAll(pageable)
                .map(MemberChargeResult::from);
    }

    @Transactional(readOnly = true)
    public Page<MemberChargeResult> findMine(
            Long authenticatedUserId,
            MemberChargeStatus status,
            Pageable pageable
    ) {
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId cannot be null");
        Objects.requireNonNull(pageable, "pageable cannot be null");

        if (status != null) {
            return memberChargeRepository.findByUserIdAndStatus(
                            authenticatedUserId,
                            status,
                            pageable
                    )
                    .map(MemberChargeResult::from);
        }

        return memberChargeRepository.findByUserId(authenticatedUserId, pageable)
                .map(MemberChargeResult::from);
    }

    @Transactional(readOnly = true)
    public MemberChargeResult findById(Long id) {
        MemberCharge memberCharge = findMemberChargeOrThrow(id);

        return MemberChargeResult.from(memberCharge);
    }

    @Transactional(readOnly = true)
    public MemberChargeResult findMineById(
            Long authenticatedUserId,
            Long memberChargeId
    ) {
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId cannot be null");

        MemberCharge memberCharge = findMemberChargeOrThrow(memberChargeId);

        if (!memberCharge.getUserId().equals(authenticatedUserId)) {
            throw new MemberChargeAccessDeniedException(
                    "Member charge does not belong to authenticated user."
            );
        }

        return MemberChargeResult.from(memberCharge);
    }

    @Transactional
    public MemberChargeResult updateFinalAmount(
            Long id,
            BigDecimal finalAmount
    ) {
        MemberCharge memberCharge = findMemberChargeOrThrow(id);

        memberCharge.updateFinalAmount(
                finalAmount,
                Instant.now(clock)
        );

        MemberCharge savedMemberCharge = memberChargeRepository.save(memberCharge);

        return MemberChargeResult.from(savedMemberCharge);
    }

    @Transactional
    public MemberChargeResult markAsOverdue(Long id) {
        MemberCharge memberCharge = findMemberChargeOrThrow(id);

        memberCharge.markAsOverdue(
                LocalDate.now(clock),
                Instant.now(clock)
        );

        MemberCharge savedMemberCharge = memberChargeRepository.save(memberCharge);

        return MemberChargeResult.from(savedMemberCharge);
    }

    @Transactional
    public MemberChargeResult expire(Long id) {
        MemberCharge memberCharge = findMemberChargeOrThrow(id);

        memberCharge.expire(
                LocalDate.now(clock),
                Instant.now(clock)
        );

        MemberCharge savedMemberCharge = memberChargeRepository.save(memberCharge);

        return MemberChargeResult.from(savedMemberCharge);
    }

    @Transactional
    public RefreshMemberChargeStatusesResult refreshOpenChargeStatuses() {
        LocalDate today = LocalDate.now(clock);
        Instant now = Instant.now(clock);

        List<MemberCharge> openCharges = findOpenChargesForStatusRefresh();

        int markedOverdueCharges = 0;
        int expiredCharges = 0;
        int unchangedCharges = 0;

        for (MemberCharge memberCharge : openCharges) {
            if (memberCharge.shouldExpireAt(today)) {
                memberCharge.expire(today, now);
                memberChargeRepository.save(memberCharge);
                expiredCharges++;
                continue;
            }

            if (memberCharge.shouldBecomeOverdueAt(today)) {
                memberCharge.markAsOverdue(today, now);
                memberChargeRepository.save(memberCharge);
                markedOverdueCharges++;
                continue;
            }

            unchangedCharges++;
        }

        return new RefreshMemberChargeStatusesResult(
                openCharges.size(),
                markedOverdueCharges,
                expiredCharges,
                unchangedCharges
        );
    }

    @Transactional
    public MemberChargeResult cancel(Long id) {
        MemberCharge memberCharge = findMemberChargeOrThrow(id);

        memberCharge.cancel(Instant.now(clock));

        MemberCharge savedMemberCharge = memberChargeRepository.save(memberCharge);

        return MemberChargeResult.from(savedMemberCharge);
    }

    private List<MemberCharge> findOpenChargesForStatusRefresh() {
        List<MemberCharge> openCharges = new ArrayList<>();

        openCharges.addAll(memberChargeRepository.findByStatus(
                        MemberChargeStatus.PENDING,
                        Pageable.unpaged()
                )
                .getContent());

        openCharges.addAll(memberChargeRepository.findByStatus(
                        MemberChargeStatus.OVERDUE,
                        Pageable.unpaged()
                )
                .getContent());

        return openCharges;
    }

    private MemberCharge findMemberChargeOrThrow(Long id) {
        Objects.requireNonNull(id, "id cannot be null");

        return memberChargeRepository.findById(id)
                .orElseThrow(() -> new MemberChargeNotFoundException(
                        "Member charge not found."
                ));
    }
}