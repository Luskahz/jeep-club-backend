package com.jeepclub.backend.memberships.core.domain.model;

import com.jeepclub.backend.memberships.core.domain.exception.MemberRegistryException;
import java.time.Instant;
import java.util.Objects;

public record Member(Long id, Long identityUserId, String membershipNumber, Instant createdAt, Instant updatedAt, Long version) {
    public Member {
        if (identityUserId == null || identityUserId <= 0) throw new MemberRegistryException(MemberRegistryException.Kind.INVALID);
        membershipNumber = new MembershipNumber(membershipNumber).value();
        Objects.requireNonNull(createdAt); Objects.requireNonNull(updatedAt);
    }
    public static Member create(Long userId, String number, Instant now) { return new Member(null, userId, number, now, now, null); }
    public Member correct(String number, long expectedVersion, Instant now) {
        if (version == null || version != expectedVersion) throw new MemberRegistryException(MemberRegistryException.Kind.CONFLICT);
        return new Member(id, identityUserId, number, createdAt, now, version);
    }
}
