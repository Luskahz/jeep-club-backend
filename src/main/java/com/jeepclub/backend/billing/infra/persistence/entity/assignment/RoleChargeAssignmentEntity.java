package com.jeepclub.backend.billing.infra.persistence.entity.assignment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(
        name = "billing_role_charge_assignments",
        indexes = {
                @Index(
                        name = "idx_billing_role_charge_assignments_role",
                        columnList = "role_id"
                )
        }
)
@DiscriminatorValue("ROLE")
@PrimaryKeyJoinColumn(name = "charge_assignment_id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleChargeAssignmentEntity extends ChargeAssignmentEntity {

    @Column(
            name = "role_id",
            nullable = false
    )
    private Long roleId;

    public RoleChargeAssignmentEntity(
            Long id,
            Long chargeDefinitionId,
            Long roleId,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        super(
                id,
                chargeDefinitionId,
                active,
                createdAt,
                updatedAt
        );

        this.roleId = roleId;
    }
}