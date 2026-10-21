package com.jeepclub.backend.iam.authorization.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name="authorization_role_deletion_history", indexes=@Index(name="ix_role_deletion_history_time", columnList="deleted_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RoleDeletionHistoryEntity {
    @Id
    @Column(name = "original_id", nullable = false)
    private Long originalId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", nullable = true, length = 255)
    private String description;

    @Column(name = "kind", nullable = false, length = 30)
    private String kind;

    @Column(name = "previous_status", nullable = false, length = 30)
    private String previousStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = true)
    private Instant updatedAt;

    @Column(name = "archived_at", nullable = true)
    private Instant archivedAt;

    @Column(name = "deleted_by_user_id", nullable = true)
    private Long deletedByUserId;

    @Column(name = "deleted_at", nullable = false)
    private Instant deletedAt;

    @Column(name = "reason", nullable = false, length = 40)
    private String reason;

    @Column(name = "inferred_time", nullable = false)
    private boolean inferredTime;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;
}
