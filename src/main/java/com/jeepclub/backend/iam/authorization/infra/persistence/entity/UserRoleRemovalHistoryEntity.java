package com.jeepclub.backend.iam.authorization.infra.persistence.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
@Entity @Table(name="authorization_user_role_removal_history", indexes=@Index(name="ix_user_role_removal_user",columnList="user_id"))
@Getter @NoArgsConstructor(access=AccessLevel.PROTECTED) @AllArgsConstructor
public class UserRoleRemovalHistoryEntity {
    @Id @Column(name="original_id",nullable=false) private Long originalId;
    @Column(name="role_id",nullable=false) private Long roleId;
    @Column(name="role_name",nullable=false,length=100) private String roleName;
    @Column(name="user_id",nullable=false) private Long userId;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="deleted_by_user_id",nullable=false) private Long deletedByUserId;
    @Column(name="deleted_at",nullable=false) private Instant deletedAt;
    @Column(name="reason",nullable=false,length=40) private String reason;
    @Column(name="schema_version",nullable=false) private int schemaVersion;
}
