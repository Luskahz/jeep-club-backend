package com.jeepclub.backend.billing.infra.persistence.entity;

import com.jeepclub.backend.billing.core.domain.enums.refund.MemberRefundStatus;
import com.jeepclub.backend.billing.core.domain.enums.refund.RefundReason;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Entity
@Table(
        name = "billing_member_refunds",
        indexes = {
                @Index(
                        name = "idx_billing_member_refunds_charge",
                        columnList = "member_charge_id"
                ),
                @Index(
                        name = "idx_billing_member_refunds_payment",
                        columnList = "member_payment_id"
                ),
                @Index(
                        name = "idx_billing_member_refunds_cycle",
                        columnList = "charge_cycle_id"
                ),
                @Index(
                        name = "idx_billing_member_refunds_user",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_billing_member_refunds_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_billing_member_refunds_eligible_until",
                        columnList = "eligible_until"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MemberRefundEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "member_charge_id",
            nullable = false
    )
    private Long memberChargeId;

    @Column(
            name = "member_payment_id",
            nullable = false
    )
    private Long memberPaymentId;

    @Column(
            name = "charge_cycle_id",
            nullable = false
    )
    private Long chargeCycleId;

    @Column(
            name = "user_id",
            nullable = false
    )
    private Long userId;

    @Column(
            name = "amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "reason",
            nullable = false,
            length = 40
    )
    private RefundReason reason;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 40
    )
    private MemberRefundStatus status;

    @Column(name = "eligible_at")
    private Instant eligibleAt;

    @Column(name = "eligible_until")
    private Instant eligibleUntil;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

    @Column(name = "requested_at")
    private Instant requestedAt;

    @Column(name = "requested_by_user_id")
    private Long requestedByUserId;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejected_by_user_id")
    private Long rejectedByUserId;

    @Column(
            name = "rejection_reason",
            length = 500
    )
    private String rejectionReason;

    @Column(name = "refunded_at")
    private Instant refundedAt;

    @Column(name = "refunded_by_user_id")
    private Long refundedByUserId;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "canceled_by_user_id")
    private Long canceledByUserId;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}