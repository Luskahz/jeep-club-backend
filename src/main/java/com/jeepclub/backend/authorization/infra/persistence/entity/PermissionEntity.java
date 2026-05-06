package com.jeepclub.backend.authorization.infra.persistence.entity;

import com.jeepclub.backend.authorization.core.domain.enums.ModuleCode;
import com.jeepclub.backend.authorization.core.domain.enums.PermissionCode;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "authorization_permissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_authorization_permissions_code",
                        columnNames = "code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_authorization_permissions_module",
                        columnList = "module"
                )
        }
)
@NoArgsConstructor
@AllArgsConstructor
public class PermissionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "code",
            nullable = false,
            length = 100
    )
    private PermissionCode code;

    @Column(
            name = "description",
            nullable = false,
            length = 255
    )
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "module",
            nullable = false,
            length = 50
    )
    private ModuleCode module;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;
}