package com.jeepclub.backend.billing.infra.persistence.mapper;

import com.jeepclub.backend.billing.core.domain.model.MemberCharge;
import com.jeepclub.backend.billing.infra.persistence.entity.MemberChargeEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class MemberChargeMapper {

    public MemberCharge toDomain(MemberChargeEntity entity) {
        Objects.requireNonNull(entity, "MemberChargeEntity cannot be null");

        return MemberCharge.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getChargeDefinitionId(),
                entity.getChargeCycleId(),
                entity.getOriginalAmount(),
                entity.getFinalAmount(),
                entity.getDueDate(),
                entity.getPaymentAcceptancePolicy(),
                entity.getLatePaymentGraceDays(),
                entity.getPaymentAllowedUntil(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getPaidAt(),
                entity.getCanceledAt()
        );
    }

    public MemberChargeEntity toEntity(MemberCharge memberCharge) {
        Objects.requireNonNull(memberCharge, "MemberCharge cannot be null");

        return new MemberChargeEntity(
                memberCharge.getId(),
                memberCharge.getUserId(),
                memberCharge.getChargeDefinitionId(),
                memberCharge.getChargeCycleId(),
                memberCharge.getOriginalAmount(),
                memberCharge.getFinalAmount(),
                memberCharge.getDueDate(),
                memberCharge.getPaymentAcceptancePolicy(),
                memberCharge.getLatePaymentGraceDays(),
                memberCharge.getPaymentAllowedUntil(),
                memberCharge.getStatus(),
                memberCharge.getCreatedAt(),
                memberCharge.getUpdatedAt(),
                memberCharge.getPaidAt(),
                memberCharge.getCanceledAt()
        );
    }
}