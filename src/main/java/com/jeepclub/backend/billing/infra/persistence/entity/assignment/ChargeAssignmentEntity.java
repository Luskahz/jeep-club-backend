package com.jeepclub.backend.billing.infra.persistence.entity.assignment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(
        name = "billing_charge_assignments",
        indexes = {
                @Index(
                        name = "idx_billing_charge_assignments_definition",
                        columnList = "charge_definition_id"
                ),
                @Index(
                        name = "idx_billing_charge_assignments_active",
                        columnList = "active"
                ),
                @Index(
                        name = "idx_billing_charge_assignments_type",
                        columnList = "assignment_type"
                )
        }
)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(
        name = "assignment_type",
        discriminatorType = DiscriminatorType.STRING,
        length = 40
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ChargeAssignmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "charge_definition_id",
            nullable = false
    )
    private Long chargeDefinitionId;

    @Column(
            name = "active",
            nullable = false
    )
    private boolean active;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected ChargeAssignmentEntity(
            Long id,
            Long chargeDefinitionId,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.chargeDefinitionId = chargeDefinitionId;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}