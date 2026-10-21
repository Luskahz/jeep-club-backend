package com.jeepclub.backend.memberships.infra.persistence.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity @Table(name="membership_number_claims",uniqueConstraints=@UniqueConstraint(name="uk_membership_claim_number",columnNames="membership_number"))
@Getter @Setter @NoArgsConstructor
public class MembershipNumberClaimEntity {
    @Id @Column(name="claim_key", length=64, updatable=false) private String claimKey;
    @Column(name="membership_number", nullable=false, updatable=false, length=64) private String membershipNumber;
    @Column(name="member_id", updatable=false) private Long memberId;
    @Column(name="actor_user_id", nullable=false, updatable=false) private Long actorUserId;
    @Column(name="claimed_at", nullable=false, updatable=false) private Instant claimedAt;
    @Column(nullable=false, updatable=false, length=500) private String reason;
}
