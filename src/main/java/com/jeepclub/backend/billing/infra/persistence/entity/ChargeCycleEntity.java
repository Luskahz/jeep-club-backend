package com.jeepclub.backend.billing.infra.persistence.entity;

import com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType;
import com.jeepclub.backend.billing.core.domain.enums.cycle.ChargeCycleStatus;
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
        name = "billing_charge_cycles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_billing_charge_cycles_definition_code",
                        columnNames = {
                                "charge_definition_id",
                                "code"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_billing_charge_cycles_definition",
                        columnList = "charge_definition_id"
                ),
                @Index(
                        name = "idx_billing_charge_cycles_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_billing_charge_cycles_due_date",
                        columnList = "due_date"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ChargeCycleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "charge_definition_id",
            nullable = false
    )
    private Long chargeDefinitionId;

    @Column(
            name = "charge_definition_name_snapshot",
            nullable = false,
            length = 120
    )
    private String chargeDefinitionNameSnapshot;

    @Column(
            name = "charge_definition_description_snapshot",
            length = 500
    )
    private String chargeDefinitionDescriptionSnapshot;

    @Column(
            name = "charge_definition_default_amount_snapshot",
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal chargeDefinitionDefaultAmountSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "charge_definition_recurrence_type_snapshot",
            nullable = false,
            length = 30
    )
    private ChargeRecurrenceType chargeDefinitionRecurrenceTypeSnapshot;

    @Column(
            name = "charge_definition_required_snapshot",
            nullable = false
    )
    private Boolean chargeDefinitionRequiredSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "charge_definition_payment_acceptance_policy_snapshot",
            nullable = false,
            length = 40
    )
    private PaymentAcceptancePolicy chargeDefinitionPaymentAcceptancePolicySnapshot;

    @Column(name = "charge_definition_late_payment_grace_days_snapshot")
    private Integer chargeDefinitionLatePaymentGraceDaysSnapshot;

    @Column(
            name = "code",
            nullable = false,
            length = 80
    )
    private String code;

    @Column(
            name = "due_date",
            nullable = false
    )
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private ChargeCycleStatus status;

    @Column(name = "generated_by_user_id")
    private Long generatedByUserId;

    @Column(
            name = "generated_at",
            nullable = false
    )
    private Instant generatedAt;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "canceled_by_user_id")
    private Long canceledByUserId;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "finished_by_user_id")
    private Long finishedByUserId;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "archived_by_user_id")
    private Long archivedByUserId;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}