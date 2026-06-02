package com.jeepclub.backend.billing.infra.persistence.mapper;

import com.jeepclub.backend.billing.core.domain.model.MemberPayment;
import com.jeepclub.backend.billing.infra.persistence.entity.MemberPaymentEntity;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class MemberPaymentMapper {

    public MemberPayment toDomain(MemberPaymentEntity entity) {
        Objects.requireNonNull(entity, "MemberPaymentEntity cannot be null");

        return MemberPayment.reconstitute(
                entity.getId(),
                entity.getMemberChargeId(),
                entity.getAmount(),
                entity.getPaymentMethod(),
                entity.getStatus(),
                entity.getPaidAt(),
                entity.getReceiptStorageKey(),
                entity.getReceiptUrl(),
                entity.getConfirmedAt(),
                entity.getConfirmedByUserId(),
                entity.getRejectedAt(),
                entity.getRejectedByUserId(),
                entity.getRejectionReason(),
                entity.getCanceledAt(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public MemberPaymentEntity toEntity(MemberPayment memberPayment) {
        Objects.requireNonNull(memberPayment, "MemberPayment cannot be null");

        return new MemberPaymentEntity(
                memberPayment.getId(),
                memberPayment.getMemberChargeId(),
                memberPayment.getAmount(),
                memberPayment.getPaymentMethod(),
                memberPayment.getStatus(),
                memberPayment.getPaidAt(),
                memberPayment.getReceiptStorageKey(),
                memberPayment.getReceiptUrl(),
                memberPayment.getConfirmedAt(),
                memberPayment.getConfirmedByUserId(),
                memberPayment.getRejectedAt(),
                memberPayment.getRejectedByUserId(),
                memberPayment.getRejectionReason(),
                memberPayment.getCanceledAt(),
                memberPayment.getNotes(),
                memberPayment.getCreatedAt(),
                memberPayment.getUpdatedAt()
        );
    }
}