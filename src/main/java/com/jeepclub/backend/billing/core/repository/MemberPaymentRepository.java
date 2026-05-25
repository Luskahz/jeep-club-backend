package com.jeepclub.backend.billing.core.repository;

import com.jeepclub.backend.billing.core.domain.enums.MemberPaymentStatus;
import com.jeepclub.backend.billing.core.domain.model.MemberPayment;

import java.util.List;
import java.util.Optional;

public interface MemberPaymentRepository {

    MemberPayment save(MemberPayment memberPayment);

    Optional<MemberPayment> findById(Long id);

    List<MemberPayment> findByMemberChargeId(Long memberChargeId);

    List<MemberPayment> findByStatus(MemberPaymentStatus status);
}