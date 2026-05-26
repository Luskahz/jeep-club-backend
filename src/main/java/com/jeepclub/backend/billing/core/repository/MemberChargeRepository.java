package com.jeepclub.backend.billing.core.repository;

import com.jeepclub.backend.billing.core.domain.enums.MemberChargeStatus;
import com.jeepclub.backend.billing.core.domain.model.MemberCharge;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface MemberChargeRepository {

    MemberCharge save(MemberCharge memberCharge);

    Optional<MemberCharge> findById(Long id);

    Page<MemberCharge> findAll(Pageable pageable);

    Page<MemberCharge> findByStatus(
            MemberChargeStatus status,
            Pageable pageable
    );

    Page<MemberCharge> findByUserId(
            Long userId,
            Pageable pageable
    );

    Page<MemberCharge> findByUserIdAndStatus(
            Long userId,
            MemberChargeStatus status,
            Pageable pageable
    );

    Page<MemberCharge> findByChargeCycleId(
            Long chargeCycleId,
            Pageable pageable
    );

    List<MemberCharge> findOpenByChargeCycleId(Long chargeCycleId);

    boolean existsByUserIdAndChargeCycleId(
            Long userId,
            Long chargeCycleId
    );
}