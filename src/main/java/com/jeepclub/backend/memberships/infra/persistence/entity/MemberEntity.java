package com.jeepclub.backend.memberships.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity @Table(name="membership_members",uniqueConstraints={@UniqueConstraint(name="uk_membership_member_identity",columnNames="identity_user_id"),@UniqueConstraint(name="uk_membership_member_number",columnNames="membership_number")})
@Getter @Setter @NoArgsConstructor
public class MemberEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="identity_user_id", nullable=false, updatable=false) private Long identityUserId;
    @Column(name="membership_number", nullable=false, length=64) private String membershipNumber;
    @Column(name="created_at", nullable=false, updatable=false) private Instant createdAt;
    @Column(name="updated_at", nullable=false) private Instant updatedAt;
    @Version @Column(nullable=false) private Long version;
}
