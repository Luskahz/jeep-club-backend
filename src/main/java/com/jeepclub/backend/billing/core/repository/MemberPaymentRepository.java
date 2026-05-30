package com.jeepclub.backend.billing.core.repository;

import com.jeepclub.backend.billing.core.domain.enums.payment.MemberPaymentStatus;
import com.jeepclub.backend.billing.core.domain.model.MemberPayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberPaymentRepository {

    MemberPayment save(MemberPayment memberPayment);

    Optional<MemberPayment> findById(Long id);

    Page<MemberPayment> findAll(Pageable pageable);

    Page<MemberPayment> findByStatus(
            MemberPaymentStatus status,
            Pageable pageable
    );

    Page<MemberPayment> findByMemberChargeId(
            Long memberChargeId,
            Pageable pageable
    );

    List<MemberPayment> findByMemberChargeIdAndStatus(
            Long memberChargeId,
            MemberPaymentStatus status
    );

    List<MemberPayment> findByMemberChargeIdAndStatusIn(
            Long memberChargeId,
            Collection<MemberPaymentStatus> statuses
    );

    boolean existsByMemberChargeIdAndStatus(
            Long memberChargeId,
            MemberPaymentStatus status
    );
}