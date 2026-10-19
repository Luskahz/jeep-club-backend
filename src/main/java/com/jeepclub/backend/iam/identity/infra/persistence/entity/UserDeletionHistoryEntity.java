package com.jeepclub.backend.iam.identity.infra.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.math.BigDecimal;
@Entity @Table(name="identity_user_deletion_history", indexes=@Index(name="ix_user_deletion_history_time", columnList="deleted_at")) @Getter @NoArgsConstructor(access=AccessLevel.PROTECTED) @AllArgsConstructor
public class UserDeletionHistoryEntity {
    @Id
    @Column(name="original_id", nullable=false)
    private Long originalId;

    @Column(name="previous_status", nullable=false, length=20)
    private String previousStatus;

    @Column(name="created_at", nullable=false)
    private Instant createdAt;

    @Column(name="disabled_at", nullable=true)
    private Instant disabledAt;

    @Column(name="archived_at", nullable=true)
    private Instant archivedAt;

    @Column(name="updated_at", nullable=true)
    private Instant updatedAt;

    @Column(name="deleted_by_user_id", nullable=false)
    private Long deletedByUserId;

    @Column(name="deleted_at", nullable=false)
    private Instant deletedAt;

    @Column(name="reason", nullable=false, length=40)
    private String reason;

    @Column(name="schema_version", nullable=false)
    private int schemaVersion;
}
