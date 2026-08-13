package com.jeepclub.backend.memberships.core.domain.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MembershipApplicantBlock {

    private Long id;
    private String cpf;
    private String reason;
    private Instant blockedAt;
    private Long blockedByUserId;
    private Instant unblockedAt;
    private Long unblockedByUserId;

    public static MembershipApplicantBlock create(
            String cpf,
            String reason,
            Instant blockedAt,
            Long blockedByUserId
    ) {
        Objects.requireNonNull(blockedAt, "blockedAt must not be null");
        Objects.requireNonNull(blockedByUserId, "blockedByUserId must not be null");

        MembershipApplicantBlock block = new MembershipApplicantBlock();
        block.cpf = normalizeCpf(cpf);
        block.reason = requireText(reason, "reason");
        block.blockedAt = blockedAt;
        block.blockedByUserId = blockedByUserId;
        return block;
    }

    public static MembershipApplicantBlock reconstitute(
            Long id,
            String cpf,
            String reason,
            Instant blockedAt,
            Long blockedByUserId,
            Instant unblockedAt,
            Long unblockedByUserId
    ) {
        MembershipApplicantBlock block = new MembershipApplicantBlock();
        block.id = id;
        block.cpf = cpf;
        block.reason = reason;
        block.blockedAt = blockedAt;
        block.blockedByUserId = blockedByUserId;
        block.unblockedAt = unblockedAt;
        block.unblockedByUserId = unblockedByUserId;
        return block;
    }

    public void unblock(Long userId, Instant now) {
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(now, "now must not be null");

        if (unblockedAt != null) {
            throw new IllegalStateException("Membership applicant block is already inactive");
        }

        this.unblockedAt = now;
        this.unblockedByUserId = userId;
    }

    private static String normalizeCpf(String cpf) {
        String normalized = requireText(cpf, "cpf").replaceAll("\\D", "");
        if (normalized.length() != 11) {
            throw new IllegalArgumentException("cpf must contain 11 digits");
        }
        return normalized;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}
