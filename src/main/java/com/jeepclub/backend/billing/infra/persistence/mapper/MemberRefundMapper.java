package com.jeepclub.backend.billing.infra.persistence.mapper;

import com.jeepclub.backend.billing.core.domain.model.MemberRefund;
import com.jeepclub.backend.billing.infra.persistence.entity.MemberRefundEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class MemberRefundMapper {

    public MemberRefund toDomain(MemberRefundEntity entity) {
        Objects.requireNonNull(entity, "MemberRefundEntity cannot be null");

        return MemberRefund.reconstitute(
                entity.getId(),
                entity.getMemberChargeId(),
                entity.getMemberPaymentId(),
                entity.getChargeCycleId(),
                entity.getUserId(),
                entity.getAmount(),
                entity.getReason(),
                entity.getStatus(),
                entity.getEligibleAt(),
                entity.getEligibleUntil(),
                entity.getCreatedByUserId(),
                entity.getRequestedAt(),
                entity.getRequestedByUserId(),
                entity.getApprovedAt(),
                entity.getApprovedByUserId(),
                entity.getRejectedAt(),
                entity.getRejectedByUserId(),
                entity.getRejectionReason(),
                entity.getRefundedAt(),
                entity.getRefundedByUserId(),
                entity.getCanceledAt(),
                entity.getCanceledByUserId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public MemberRefundEntity toEntity(MemberRefund memberRefund) {
        Objects.requireNonNull(memberRefund, "MemberRefund cannot be null");

        return new MemberRefundEntity(
                memberRefund.getId(),
                memberRefund.getMemberChargeId(),
                memberRefund.getMemberPaymentId(),
                memberRefund.getChargeCycleId(),
                memberRefund.getUserId(),
                memberRefund.getAmount(),
                memberRefund.getReason(),
                memberRefund.getStatus(),
                memberRefund.getEligibleAt(),
                memberRefund.getEligibleUntil(),
                memberRefund.getCreatedByUserId(),
                memberRefund.getRequestedAt(),
                memberRefund.getRequestedByUserId(),
                memberRefund.getApprovedAt(),
                memberRefund.getApprovedByUserId(),
                memberRefund.getRejectedAt(),
                memberRefund.getRejectedByUserId(),
                memberRefund.getRejectionReason(),
                memberRefund.getRefundedAt(),
                memberRefund.getRefundedByUserId(),
                memberRefund.getCanceledAt(),
                memberRefund.getCanceledByUserId(),
                memberRefund.getCreatedAt(),
                memberRefund.getUpdatedAt()
        );
    }
}