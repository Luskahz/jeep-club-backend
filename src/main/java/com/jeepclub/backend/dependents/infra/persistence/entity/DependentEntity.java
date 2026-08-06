package com.jeepclub.backend.dependents.infra.persistence.entity;

import com.jeepclub.backend.authentication.infra.persistence.entity.UserEntity;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(
        name = "membership_dependents",
        indexes = {
                @Index(name = "idx_dependent_socio", columnList = "socio_id"),
                @Index(name = "idx_dependent_cpf", columnList = "cpf", unique = true)
        }
)
@Getter
@Setter
public class DependentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(length = 11, unique = true)
    private String cpf;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false, length = 50)
    private RelationshipType relationshipType;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    // === LGPD Consent (RN013) ===
    @Column(name = "consent_accepted", nullable = false)
    private boolean consentAccepted;

    @Column(name = "consent_accepted_at")
    private Instant consentAcceptedAt;

    // === Relationship with Sócio (User titular) ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "socio_id", nullable = false)
    private UserEntity socio;

    // === Auditing ===
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
