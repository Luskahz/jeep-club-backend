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
        name = "dependents_dependent_history",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_dependent_history_dependent_id",
                        columnNames = "dependent_id"
                )
        },
        indexes = {
                @Index(
                        name = "idx_dependent_history_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_dependent_history_cpf",
                        columnList = "cpf"
                ),
                @Index(
                        name = "idx_dependent_history_deleted_at",
                        columnList = "deleted_at"
                )
        }
)
public class DependentHistoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "dependent_id",
            nullable = false
    )
    private Long dependentId;

    @Column(
            nullable = false,
            length = 150
    )
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

    @Column(
            name = "user_id",
            nullable = false
    )
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private DependentStatus status;

    @Column(
            name = "deleted_by_user_id",
            nullable = false
    )
    private Long deletedByUserId;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(
            name = "updated_at"
    )
    private Instant updatedAt;

    @Column(
            name = "deleted_at",
            nullable = false,
            updatable = false
    )
    private Instant deletedAt;
}
