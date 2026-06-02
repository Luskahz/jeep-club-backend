package com.jeepclub.backend.billing.infra.persistence.adapter;

import com.jeepclub.backend.billing.core.domain.enums.refund.MemberRefundStatus;
import com.jeepclub.backend.billing.core.domain.model.MemberRefund;
import com.jeepclub.backend.billing.core.repository.MemberRefundRepository;
import com.jeepclub.backend.billing.infra.persistence.entity.MemberRefundEntity;
import com.jeepclub.backend.billing.infra.persistence.jpa.MemberRefundJpaRepository;
import com.jeepclub.backend.billing.infra.persistence.mapper.MemberRefundMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberRefundAdapter implements MemberRefundRepository {

    private static final List<MemberRefundStatus> ACTIVE_REFUND_STATUSES = List.of(
            MemberRefundStatus.ELIGIBLE,
            MemberRefundStatus.REQUESTED,
            MemberRefundStatus.APPROVED
    );

    private final MemberRefundJpaRepository jpa;
    private final MemberRefundMapper mapper;

    @Override
    public MemberRefund save(MemberRefund memberRefund) {
        MemberRefundEntity entity = mapper.toEntity(memberRefund);
        MemberRefundEntity savedEntity = jpa.save(entity);

        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<MemberRefund> findById(Long id) {
        return jpa.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Page<MemberRefund> findAll(Pageable pageable) {
        return jpa.findAll(pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<MemberRefund> findByStatus(
            MemberRefundStatus status,
            Pageable pageable
    ) {
        return jpa.findByStatus(status, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<MemberRefund> findByUserId(
            Long userId,
            Pageable pageable
    ) {
        return jpa.findByUserId(userId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<MemberRefund> findByChargeCycleId(
            Long chargeCycleId,
            Pageable pageable
    ) {
        return jpa.findByChargeCycleId(chargeCycleId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<MemberRefund> findActiveByMemberPaymentId(Long memberPaymentId) {
        return jpa.findFirstByMemberPaymentIdAndStatusIn(
                        memberPaymentId,
                        ACTIVE_REFUND_STATUSES
                )
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsActiveByMemberPaymentId(Long memberPaymentId) {
        return jpa.existsByMemberPaymentIdAndStatusIn(
                memberPaymentId,
                ACTIVE_REFUND_STATUSES
        );
    }

    @Override
    public boolean existsRefundedByMemberPaymentId(Long memberPaymentId) {
        return jpa.existsByMemberPaymentIdAndStatus(
                memberPaymentId,
                MemberRefundStatus.REFUNDED
        );
    }
}