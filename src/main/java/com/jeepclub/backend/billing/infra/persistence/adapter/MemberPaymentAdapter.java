package com.jeepclub.backend.billing.infra.persistence.adapter;

import com.jeepclub.backend.billing.core.domain.enums.payment.MemberPaymentStatus;
import com.jeepclub.backend.billing.core.domain.model.MemberPayment;
import com.jeepclub.backend.billing.core.repository.MemberPaymentRepository;
import com.jeepclub.backend.billing.infra.persistence.entity.MemberPaymentEntity;
import com.jeepclub.backend.billing.infra.persistence.jpa.MemberPaymentJpaRepository;
import com.jeepclub.backend.billing.infra.persistence.mapper.MemberPaymentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberPaymentAdapter implements MemberPaymentRepository {

    private final MemberPaymentJpaRepository jpa;
    private final MemberPaymentMapper mapper;

    @Override
    public MemberPayment save(MemberPayment memberPayment) {
        MemberPaymentEntity entity = mapper.toEntity(memberPayment);
        MemberPaymentEntity savedEntity = jpa.save(entity);

        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<MemberPayment> findById(Long id) {
        return jpa.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<MemberPayment> findByIdForUpdate(Long id) {
        return jpa.findWithLockingById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Page<MemberPayment> findAll(Pageable pageable) {
        return jpa.findAll(pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<MemberPayment> findByStatus(
            MemberPaymentStatus status,
            Pageable pageable
    ) {
        return jpa.findByStatus(status, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<MemberPayment> findByMemberChargeId(
            Long memberChargeId,
            Pageable pageable
    ) {
        return jpa.findByMemberChargeId(memberChargeId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public List<MemberPayment> findByMemberChargeIdAndStatus(
            Long memberChargeId,
            MemberPaymentStatus status
    ) {
        return jpa.findByMemberChargeIdAndStatus(memberChargeId, status)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<MemberPayment> findByMemberChargeIdAndStatusIn(
            Long memberChargeId,
            Collection<MemberPaymentStatus> statuses
    ) {
        return jpa.findByMemberChargeIdAndStatusIn(memberChargeId, statuses)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByMemberChargeIdAndStatus(
            Long memberChargeId,
            MemberPaymentStatus status
    ) {
        return jpa.existsByMemberChargeIdAndStatus(
                memberChargeId,
                status
        );
    }
}