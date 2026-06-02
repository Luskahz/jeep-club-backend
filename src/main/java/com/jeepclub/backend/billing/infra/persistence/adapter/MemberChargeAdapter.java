package com.jeepclub.backend.billing.infra.persistence.adapter;

import com.jeepclub.backend.billing.core.domain.enums.charge.MemberChargeStatus;
import com.jeepclub.backend.billing.core.domain.model.MemberCharge;
import com.jeepclub.backend.billing.core.repository.MemberChargeRepository;
import com.jeepclub.backend.billing.infra.persistence.entity.MemberChargeEntity;
import com.jeepclub.backend.billing.infra.persistence.jpa.MemberChargeJpaRepository;
import com.jeepclub.backend.billing.infra.persistence.mapper.MemberChargeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberChargeAdapter implements MemberChargeRepository {

    private final MemberChargeJpaRepository jpa;
    private final MemberChargeMapper mapper;

    @Override
    public MemberCharge save(MemberCharge memberCharge) {
        MemberChargeEntity entity = mapper.toEntity(memberCharge);
        MemberChargeEntity savedEntity = jpa.save(entity);

        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<MemberCharge> findById(Long id) {
        return jpa.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<MemberCharge> findByIdForUpdate(Long id) {
        return jpa.findWithLockingById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Page<MemberCharge> findAll(Pageable pageable) {
        return jpa.findAll(pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<MemberCharge> findByStatus(
            MemberChargeStatus status,
            Pageable pageable
    ) {
        return jpa.findByStatus(status, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<MemberCharge> findByUserId(
            Long userId,
            Pageable pageable
    ) {
        return jpa.findByUserId(userId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<MemberCharge> findByUserIdAndStatus(
            Long userId,
            MemberChargeStatus status,
            Pageable pageable
    ) {
        return jpa.findByUserIdAndStatus(userId, status, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Page<MemberCharge> findByChargeCycleId(
            Long chargeCycleId,
            Pageable pageable
    ) {
        return jpa.findByChargeCycleId(chargeCycleId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public List<MemberCharge> findByChargeCycleId(Long chargeCycleId) {
        return jpa.findByChargeCycleId(chargeCycleId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<MemberCharge> findOpenByChargeCycleId(Long chargeCycleId) {
        return jpa.findByChargeCycleIdAndStatus(
                        chargeCycleId,
                        MemberChargeStatus.PENDING
                )
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByUserIdAndChargeCycleId(
            Long userId,
            Long chargeCycleId
    ) {
        return jpa.existsByUserIdAndChargeCycleId(
                userId,
                chargeCycleId
        );
    }
}