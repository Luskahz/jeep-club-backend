package com.jeepclub.backend.billing.core.repository;

import com.jeepclub.backend.billing.core.domain.enums.MemberChargeStatus;
import com.jeepclub.backend.billing.core.domain.model.MemberCharge;

import java.util.List;
import java.util.Optional;

public interface MemberChargeRepository {

    MemberCharge save(MemberCharge memberCharge);

    Optional<MemberCharge> findById(Long id);

    List<MemberCharge> findByUserId(Long userId);

    List<MemberCharge> findByUserIdAndStatus(
            Long userId,
            MemberChargeStatus status
    );

    boolean existsByUserIdAndChargeDefinitionId(
            Long userId,
            Long chargeDefinitionId
    );
}