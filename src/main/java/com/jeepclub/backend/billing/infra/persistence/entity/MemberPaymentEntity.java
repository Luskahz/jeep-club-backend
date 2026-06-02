package com.jeepclub.backend.billing.infra.persistence.entity;

import com.jeepclub.backend.billing.core.domain.enums.payment.MemberPaymentStatus;
import com.jeepclub.backend.billing.core.domain.enums.payment.PaymentMethod;
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
        name = "billing_member_payments",
        indexes = {
                @Index(
                        name = "idx_billing_member_payments_charge",
                        columnList = "member_charge_id"
                ),
                @Index(
                        name = "idx_billing_member_payments_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_billing_member_payments_paid_at",
                        columnList = "paid_at"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MemberPaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "member_charge_id",
            nullable = false
    )
    private Long memberChargeId;

    @Column(
            name = "amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_method",
            nullable = false,
            length = 40
    )
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 40
    )
    private MemberPaymentStatus status;

    @Column(
            name = "paid_at",
            nullable = false
    )
    private Instant paidAt;

    @Column(
            name = "receipt_storage_key",
            nullable = false,
            length = 500
    )
    private String receiptStorageKey;

    @Column(
            name = "receipt_url",
            nullable = false,
            length = 1000
    )
    private String receiptUrl;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "confirmed_by_user_id")
    private Long confirmedByUserId;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "rejected_by_user_id")
    private Long rejectedByUserId;

    @Column(
            name = "rejection_reason",
            length = 500
    )
    private String rejectionReason;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(
            name = "notes",
            length = 500
    )
    private String notes;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}