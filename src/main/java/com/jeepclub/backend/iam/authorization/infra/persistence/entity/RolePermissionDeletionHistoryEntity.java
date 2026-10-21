package com.jeepclub.backend.iam.authorization.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name="authorization_role_permission_deletion_history", indexes=@Index(name="ix_role_permission_deletion_role", columnList="role_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RolePermissionDeletionHistoryEntity {
    @Id
    @Column(name = "original_id", nullable = false)
    private Long originalId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "permission_id", nullable = false)
    private Long permissionId;

    @Column(name = "permission_code", nullable = false, length = 120)
    private String permissionCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
