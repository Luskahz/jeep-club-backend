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
        name = "authorization_role_permissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_authorization_role_permissions_role_permission",
                        columnNames = {"role_id", "permission_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_authorization_role_permissions_role_id",
                        columnList = "role_id"
                ),
                @Index(
                        name = "idx_authorization_role_permissions_permission_id",
                        columnList = "permission_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RolePermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "role_id",
            nullable = false
    )
    private Long roleId;

    @Column(
            name = "permission_id",
            nullable = false
    )
    private Long permissionId;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;
}