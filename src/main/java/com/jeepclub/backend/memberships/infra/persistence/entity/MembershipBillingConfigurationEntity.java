package com.jeepclub.backend.memberships.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "membership_billing_configurations",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_membership_billing_configuration_singleton",
                columnNames = "singleton_key"
        )
)
public class MembershipBillingConfigurationEntity {

    static final String CURRENT_KEY = "CURRENT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "singleton_key", nullable = false, updatable = false, length = 16)
    private String singletonKey = CURRENT_KEY;

    @Column(name = "charge_definition_id", nullable = false)
    private Long chargeDefinitionId;

    @Column(name = "enforcement_enabled", nullable = false)
    private boolean enforcementEnabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
