package com.jeepclub.backend.billing.infra.persistence.entity.assignment;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(
        name = "billing_user_charge_assignments",
        indexes = {
                @Index(
                        name = "idx_billing_user_charge_assignments_user",
                        columnList = "user_id"
                )
        }
)
@DiscriminatorValue("USER")
@PrimaryKeyJoinColumn(name = "charge_assignment_id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserChargeAssignmentEntity extends ChargeAssignmentEntity {

    @Column(
            name = "user_id",
            nullable = false
    )
    private Long userId;

    public UserChargeAssignmentEntity(
            Long id,
            Long chargeDefinitionId,
            Long userId,
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

        this.userId = userId;
    }
}