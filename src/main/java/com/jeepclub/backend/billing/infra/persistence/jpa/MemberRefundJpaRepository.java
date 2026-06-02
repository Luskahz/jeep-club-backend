package com.jeepclub.backend.billing.infra.persistence.jpa;

import com.jeepclub.backend.billing.core.domain.enums.refund.MemberRefundStatus;
import com.jeepclub.backend.billing.infra.persistence.entity.MemberRefundEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface MemberRefundJpaRepository extends JpaRepository<MemberRefundEntity, Long> {

    Page<MemberRefundEntity> findByStatus(
            MemberRefundStatus status,
            Pageable pageable
    );

    Page<MemberRefundEntity> findByUserId(
            Long userId,
            Pageable pageable
    );

    Page<MemberRefundEntity> findByChargeCycleId(
            Long chargeCycleId,
            Pageable pageable
    );

    Optional<MemberRefundEntity> findFirstByMemberPaymentIdAndStatusIn(
            Long memberPaymentId,
            Collection<MemberRefundStatus> statuses
    );

    boolean existsByMemberPaymentIdAndStatusIn(
            Long memberPaymentId,
            Collection<MemberRefundStatus> statuses
    );

    boolean existsByMemberPaymentIdAndStatus(
            Long memberPaymentId,
            MemberRefundStatus status
    );
}