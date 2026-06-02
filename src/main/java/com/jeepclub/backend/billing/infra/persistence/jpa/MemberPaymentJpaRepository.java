package com.jeepclub.backend.billing.infra.persistence.jpa;

import com.jeepclub.backend.billing.core.domain.enums.payment.MemberPaymentStatus;
import com.jeepclub.backend.billing.infra.persistence.entity.MemberPaymentEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MemberPaymentJpaRepository extends JpaRepository<MemberPaymentEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MemberPaymentEntity> findWithLockingById(Long id);

    Page<MemberPaymentEntity> findByStatus(
            MemberPaymentStatus status,
            Pageable pageable
    );

    Page<MemberPaymentEntity> findByMemberChargeId(
            Long memberChargeId,
            Pageable pageable
    );

    List<MemberPaymentEntity> findByMemberChargeIdAndStatus(
            Long memberChargeId,
            MemberPaymentStatus status
    );

    List<MemberPaymentEntity> findByMemberChargeIdAndStatusIn(
            Long memberChargeId,
            Collection<MemberPaymentStatus> statuses
    );

    boolean existsByMemberChargeIdAndStatus(
            Long memberChargeId,
            MemberPaymentStatus status
    );
}