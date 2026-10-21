package com.jeepclub.backend.memberships.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity @Table(name="membership_member_audits",indexes=@Index(name="ix_membership_audit_member",columnList="member_id,id"))
@Getter @Setter @NoArgsConstructor
public class MemberRegistryAuditEntity {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(name="member_id", updatable=false) private Long memberId;
    @Column(name="previous_number", updatable=false, length=64) private String previousNumber;
    @Column(name="new_number", nullable=false, updatable=false, length=64) private String newNumber;
    @Column(name="actor_user_id", nullable=false, updatable=false) private Long actorUserId;
    @Column(name="occurred_at", nullable=false, updatable=false) private Instant occurredAt;
    @Column(nullable=false, updatable=false, length=500) private String reason;
    @Column(nullable=false, updatable=false, length=32) private String operation;
    @Column(nullable=false, updatable=false, length=16) private String outcome;
}
