package com.jeepclub.backend.identity.infra.persistence.entity;

import com.jeepclub.backend.identity.api.module.IdentityStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "identity_users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_identity_users_cpf",
                        columnNames = "cpf"
                ),
                @UniqueConstraint(
                        name = "uk_identity_users_email",
                        columnNames = "email"
                ),
                @UniqueConstraint(
                        name = "uk_identity_users_rg",
                        columnNames = "rg"
                )
        },
        indexes = {
                @Index(
                        name = "idx_identity_users_status",
                        columnList = "status"
                )
        }
)
public class IdentityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 180)
    private String email;

    @Column(nullable = false, length = 11)
    private String cpf;

    @Column(length = 20)
    private String rg;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "profile_photo_url", length = 255)
    private String profilePhotoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IdentityStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "disabled_at")
    private Instant disabledAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
