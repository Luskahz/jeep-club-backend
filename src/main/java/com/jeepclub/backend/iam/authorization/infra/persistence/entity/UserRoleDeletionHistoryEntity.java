package com.jeepclub.backend.iam.authorization.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name="authorization_user_role_deletion_history", indexes=@Index(name="ix_user_role_deletion_role", columnList="role_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class UserRoleDeletionHistoryEntity {
    @Id
    @Column(name = "original_id", nullable = false)
    private Long originalId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
