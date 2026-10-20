package com.jeepclub.backend.memberships.core.port;

public interface PendingMembershipAuthentication {
    /** Caller holds the Identity lock. Missing or completed credentials are a conflict. */
    void requirePending(Long identityId);
}
