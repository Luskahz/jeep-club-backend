package com.jeepclub.backend.memberships.infra.persistence.entity;

import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "membership_applications")
public class MembershipApplicationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipApplicationStatus status;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    @Column(name = "created_user_id")
    private Long createdUserId;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private Instant requestedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private Long version;
}
