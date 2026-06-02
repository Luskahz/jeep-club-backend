package com.jeepclub.backend.billing.infra.persistence.entity;

import com.jeepclub.backend.billing.core.domain.enums.charge.MemberChargeStatus;
import com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Getter
@Entity
@Table(
        name = "billing_member_charges",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_billing_member_charges_user_cycle",
                        columnNames = {
                                "user_id",
                                "charge_cycle_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_billing_member_charges_user",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_billing_member_charges_definition",
                        columnList = "charge_definition_id"
                ),
                @Index(
                        name = "idx_billing_member_charges_cycle",
                        columnList = "charge_cycle_id"
                ),
                @Index(
                        name = "idx_billing_member_charges_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_billing_member_charges_due_date",
                        columnList = "due_date"
                ),
                @Index(
                        name = "idx_billing_member_charges_payment_allowed_until",
                        columnList = "payment_allowed_until"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MemberChargeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "user_id",
            nullable = false
    )
    private Long userId;

    @Column(
            name = "charge_definition_id",
            nullable = false
    )
    private Long chargeDefinitionId;

    @Column(
            name = "charge_cycle_id",
            nullable = false
    )
    private Long chargeCycleId;

    @Column(
            name = "original_amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal originalAmount;

    @Column(
            name = "final_amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal finalAmount;

    @Column(
            name = "due_date",
            nullable = false
    )
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_acceptance_policy",
            nullable = false,
            length = 40
    )
    private PaymentAcceptancePolicy paymentAcceptancePolicy;

    @Column(name = "late_payment_grace_days")
    private Integer latePaymentGraceDays;

    @Column(name = "payment_allowed_until")
    private LocalDate paymentAllowedUntil;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private MemberChargeStatus status;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "canceled_at")
    private Instant canceledAt;
}