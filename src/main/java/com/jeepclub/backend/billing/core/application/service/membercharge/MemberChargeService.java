package com.jeepclub.backend.billing.core.application.service.membercharge;

import com.jeepclub.backend.billing.core.application.exception.charge.MemberChargeAccessDeniedException;
import com.jeepclub.backend.billing.core.application.exception.charge.MemberChargeNotFoundException;
import com.jeepclub.backend.billing.core.application.result.charge.MemberChargeResult;
import com.jeepclub.backend.billing.core.domain.enums.charge.MemberChargeStatus;
import com.jeepclub.backend.billing.core.domain.model.MemberCharge;
import com.jeepclub.backend.billing.core.repository.MemberChargeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class MemberChargeService {

    private final MemberChargeRepository memberChargeRepository;
    private final Clock clock;

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
                    .map(this::toResult);
        }

        return memberChargeRepository.findByUserId(authenticatedUserId, pageable)
                .map(this::toResult);
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

        return toResult(memberCharge);
    }

    private MemberChargeResult toResult(MemberCharge memberCharge) {
        return MemberChargeResult.from(memberCharge, LocalDate.now(clock));
    }

    private MemberCharge findMemberChargeOrThrow(Long id) {
        Objects.requireNonNull(id, "id cannot be null");

        return memberChargeRepository.findById(id)
                .orElseThrow(() -> new MemberChargeNotFoundException(
                        "Member charge not found."
                ));
    }
}
