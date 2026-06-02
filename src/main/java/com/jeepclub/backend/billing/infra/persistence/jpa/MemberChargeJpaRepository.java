package com.jeepclub.backend.billing.infra.persistence.jpa;

import com.jeepclub.backend.billing.core.domain.enums.charge.MemberChargeStatus;
import com.jeepclub.backend.billing.infra.persistence.entity.MemberChargeEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface MemberChargeJpaRepository extends JpaRepository<MemberChargeEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MemberChargeEntity> findWithLockingById(Long id);

    Page<MemberChargeEntity> findByStatus(
            MemberChargeStatus status,
            Pageable pageable
    );

    Page<MemberChargeEntity> findByUserId(
            Long userId,
            Pageable pageable
    );

    Page<MemberChargeEntity> findByUserIdAndStatus(
            Long userId,
            MemberChargeStatus status,
            Pageable pageable
    );

    Page<MemberChargeEntity> findByChargeCycleId(
            Long chargeCycleId,
            Pageable pageable
    );

    List<MemberChargeEntity> findByChargeCycleId(Long chargeCycleId);

    List<MemberChargeEntity> findByChargeCycleIdAndStatus(
            Long chargeCycleId,
            MemberChargeStatus status
    );

    boolean existsByUserIdAndChargeCycleId(
            Long userId,
            Long chargeCycleId
    );
}