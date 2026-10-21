package com.jeepclub.backend.memberships.core.port;

import java.time.Instant;

public interface PendingMembershipIdentityReversal {
    void lock(Long userId);
    void delete(Long userId, Long actorId, Instant now);
}
