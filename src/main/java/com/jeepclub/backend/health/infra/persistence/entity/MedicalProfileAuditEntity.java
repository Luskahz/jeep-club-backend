package com.jeepclub.backend.health.infra.persistence.entity;

import com.jeepclub.backend.health.core.application.audit.MedicalProfileAuditOperation;
import com.jeepclub.backend.health.core.application.audit.MedicalProfileAuditOutcome;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "medical_profile_audit_events",
        indexes = {
                @Index(
                        name = "idx_medical_profile_audit_owner_time",
                        columnList = "owner_type,owner_id,occurred_at"
                ),
                @Index(
                        name = "idx_medical_profile_audit_actor_time",
                        columnList = "actor_user_id,occurred_at"
                )
        }
)
public class MedicalProfileAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", length = 30)
    private MedicalProfileOwnerType ownerType;

    @Column(name = "owner_id")
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MedicalProfileAuditOperation operation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MedicalProfileAuditOutcome outcome;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;
}
