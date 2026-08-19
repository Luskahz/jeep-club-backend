package com.jeepclub.backend.dependents.infra.persistence.entity;

import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import jakarta.persistence.*;
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
        name = "membership_dependents",
        indexes = {
                @Index(
                        name = "idx_dependent_user_status",
                        columnList = "user_id,status"
                ),
                @Index(
                        name = "idx_dependent_cpf_status",
                        columnList = "cpf,status"
                )
        }
)
public class DependentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(
            nullable = false,
            length = 11
    )
    private String cpf;

    @Column(
            name = "birth_date",
            nullable = false
    )
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "relationship_type",
            nullable = false,
            length = 50
    )
    private RelationshipType relationshipType;

    @Column(
            name = "phone_number",
            length = 20
    )
    private String phoneNumber;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private DependentStatus status;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
