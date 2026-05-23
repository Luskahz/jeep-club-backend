package com.jeepclub.backend.medical.infra.persistence.entity;

import com.jeepclub.backend.medical.core.domain.BloodType;
import com.jeepclub.backend.medical.core.domain.MedicalProfileOwnerType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
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
    @Column(name = "owner_type", nullable = false, length = 30)
    private MedicalProfileOwnerType ownerType;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_type", nullable = false, length = 30)
    private BloodType bloodType = BloodType.UNKNOWN;

    @Lob
    @Column(name = "allergies", columnDefinition = "TEXT")
    private String allergies;

    @Lob
    @Column(name = "chronic_conditions", columnDefinition = "TEXT")
    private String chronicConditions;

    @Lob
    @Column(name = "continuous_medications", columnDefinition = "TEXT")
    private String continuousMedications;

    @Column(name = "health_insurance_provider", length = 120)
    private String healthInsuranceProvider;

    @Column(name = "health_insurance_plan", length = 120)
    private String healthInsurancePlan;

    @Column(name = "health_insurance_number", length = 80)
    private String healthInsuranceNumber;

    @Column(name = "emergency_contact_name", length = 120)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 20)
    private String emergencyContactPhone;

    @Column(name = "emergency_contact_relationship", length = 80)
    private String emergencyContactRelationship;

    @Lob
    @Column(name = "observations", columnDefinition = "TEXT")
    private String observations;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;


    // isso não é interessante, recomendo que deixe que o model cuide disso. pois o Instant.now
    // tem que nascer no service e ser passado a diante até a finalização da requisição.
    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;

        if (this.bloodType == null) {
            this.bloodType = BloodType.UNKNOWN;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();

        if (this.bloodType == null) {
            this.bloodType = BloodType.UNKNOWN;
        }
    }
}