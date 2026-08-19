package com.jeepclub.backend.health.infra.persistence.entity;

import com.jeepclub.backend.health.core.domain.enums.BloodType;
import com.jeepclub.backend.health.core.domain.enums.MedicalProfileOwnerType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "medical_profiles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_medical_profile_owner",
                        columnNames = {"owner_type", "owner_id"}
                )
        }
)
public class MedicalProfileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "owner_type",
            nullable = false,
            length = 30
    )
    private MedicalProfileOwnerType ownerType;

    @Column(
            name = "owner_id",
            nullable = false
    )
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "blood_type",
            nullable = false,
            length = 30
    )
    private BloodType bloodType;

    @Lob
    @Column(
            name = "allergies",
            columnDefinition = "TEXT"
    )
    private String allergies;

    @Lob
    @Column(
            name = "chronic_conditions",
            columnDefinition = "TEXT"
    )
    private String chronicConditions;

    @Lob
    @Column(
            name = "continuous_medications",
            columnDefinition = "TEXT"
    )
    private String continuousMedications;

    @Column(
            name = "health_insurance_provider",
            length = 120
    )
    private String healthInsuranceProvider;

    @Column(
            name = "health_insurance_plan",
            length = 120
    )
    private String healthInsurancePlan;

    @Column(
            name = "health_insurance_number",
            length = 80
    )
    private String healthInsuranceNumber;

    @Column(
            name = "emergency_contact_name",
            length = 120
    )
    private String emergencyContactName;

    @Column(
            name = "emergency_contact_phone",
            length = 11
    )
    private String emergencyContactPhone;

    @Column(
            name = "emergency_contact_relationship",
            length = 80
    )
    private String emergencyContactRelationship;

    @Lob
    @Column(
            name = "observations",
            columnDefinition = "TEXT"
    )
    private String observations;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private Instant updatedAt;
}