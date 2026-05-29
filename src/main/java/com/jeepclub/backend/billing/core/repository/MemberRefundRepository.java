package com.jeepclub.backend.billing.core.repository;

import com.jeepclub.backend.billing.core.domain.enums.refund.MemberRefundStatus;
import com.jeepclub.backend.billing.core.domain.model.MemberRefund;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface MemberRefundRepository {

    MemberRefund save(MemberRefund memberRefund);

    Optional<MemberRefund> findById(Long id);

    Page<MemberRefund> findAll(Pageable pageable);

    Page<MemberRefund> findByStatus(
            MemberRefundStatus status,
            Pageable pageable
    );

    Page<MemberRefund> findByUserId(
            Long userId,
            Pageable pageable
    );

    Page<MemberRefund> findByChargeCycleId(
            Long chargeCycleId,
            Pageable pageable
    );

    Optional<MemberRefund> findActiveByMemberPaymentId(Long memberPaymentId);

    boolean existsActiveByMemberPaymentId(Long memberPaymentId);
}