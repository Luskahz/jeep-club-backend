package com.jeepclub.backend.billing.infra.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.math.BigDecimal;
@Entity @Table(name="billing_charge_definition_deletion_history", indexes=@Index(name="ix_definition_deletion_history_time", columnList="deleted_at")) @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED) @AllArgsConstructor
public class ChargeDefinitionDeletionHistoryEntity {
    @Id
    @Column(name="original_id", nullable=false)
    private Long originalId;

    @Column(name="name", nullable=false, length=120)
    private String name;

    @Column(name="description", nullable=true, length=500)
    private String description;

    @Column(name="default_amount", nullable=false, precision=15, scale=2)
    private BigDecimal defaultAmount;

    @Column(name="recurrence_type", nullable=false, length=30)
    private String recurrenceType;

    @Column(name="required", nullable=false)
    private Boolean required;

    @Column(name="payment_acceptance_policy", nullable=false, length=40)
    private String paymentAcceptancePolicy;

    @Column(name="late_payment_grace_days", nullable=true)
    private Integer latePaymentGraceDays;

    @Column(name="previous_status", nullable=false, length=30)
    private String previousStatus;

    @Column(name="created_at", nullable=false)
    private Instant createdAt;

    @Column(name="updated_at", nullable=true)
    private Instant updatedAt;

    @Column(name="archived_at", nullable=true)
    private Instant archivedAt;

    @Column(name="deleted_by_user_id", nullable=false)
    private Long deletedByUserId;

    @Column(name="deleted_at", nullable=false)
    private Instant deletedAt;

    @Column(name="reason", nullable=false, length=40)
    private String reason;

    @Column(name="schema_version", nullable=false)
    private int schemaVersion;
}
