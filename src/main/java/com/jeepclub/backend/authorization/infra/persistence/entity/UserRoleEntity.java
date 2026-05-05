package com.jeepclub.backend.authorization.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Entity
@Table(
        name = "authorization_user_roles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_authorization_user_roles_user_role",
                        columnNames = {"user_id", "role_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_authorization_user_roles_user_id",
                        columnList = "user_id"
                ),
                @Index(
                        name = "idx_authorization_user_roles_role_id",
                        columnList = "role_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "user_id",
            nullable = false
    )
    private Long userId;

    @Column(
            name = "role_id",
            nullable = false
    )
    private Long roleId;

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