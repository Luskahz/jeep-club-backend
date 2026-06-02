package com.jeepclub.backend.billing.infra.persistence.entity;

import com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType;
import com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy;
import com.jeepclub.backend.billing.core.domain.enums.definition.ChargeDefinitionStatus;
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
        name = "billing_charge_definitions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_billing_charge_definitions_name",
                        columnNames = "name"
                )
        },
        indexes = {
                @Index(
                        name = "idx_billing_charge_definitions_status",
                        columnList = "status"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ChargeDefinitionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "name",
            nullable = false,
            length = 120
    )
    private String name;

    @Column(
            name = "description",
            length = 500
    )
    private String description;

    @Column(
            name = "default_amount",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal defaultAmount;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "recurrence_type",
            nullable = false,
            length = 30
    )
    private ChargeRecurrenceType recurrenceType;

    @Column(
            name = "required",
            nullable = false
    )
    private Boolean required;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_acceptance_policy",
            nullable = false,
            length = 40
    )
    private PaymentAcceptancePolicy paymentAcceptancePolicy;

    @Column(name = "late_payment_grace_days")
    private Integer latePaymentGraceDays;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private ChargeDefinitionStatus status;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "archived_at")
    private Instant archivedAt;
}