package com.jeepclub.backend.billing.infra.persistence.entity.assignment;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "billing_all_members_charge_assignments")
@DiscriminatorValue("ALL_MEMBERS")
@PrimaryKeyJoinColumn(name = "charge_assignment_id")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AllMembersChargeAssignmentEntity extends ChargeAssignmentEntity {

    public AllMembersChargeAssignmentEntity(
            Long id,
            Long chargeDefinitionId,
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
    }
}