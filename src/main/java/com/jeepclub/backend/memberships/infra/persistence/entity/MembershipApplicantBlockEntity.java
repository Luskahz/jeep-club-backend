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
        name = "membership_applicant_blocks",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_membership_applicant_blocks_active_cpf",
                        columnNames = "active_cpf"
                )
        },
        indexes = {
                @Index(
                        name = "idx_membership_applicant_blocks_active_cpf",
                        columnList = "cpf, unblocked_at"
                )
        }
)
public class MembershipApplicantBlockEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 11)
    private String cpf;

    @Column(name = "active_cpf", length = 11)
    private String activeCpf;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(name = "blocked_at", nullable = false, updatable = false)
    private Instant blockedAt;

    @Column(name = "blocked_by_user_id", nullable = false, updatable = false)
    private Long blockedByUserId;

    @Column(name = "unblocked_at")
    private Instant unblockedAt;

    @Column(name = "unblocked_by_user_id")
    private Long unblockedByUserId;
}
