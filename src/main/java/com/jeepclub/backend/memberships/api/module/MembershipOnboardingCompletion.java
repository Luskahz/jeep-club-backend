package com.jeepclub.backend.memberships.api.module;

import java.time.Instant;

/** Public module boundary used after Authentication completes first access. */
public interface MembershipOnboardingCompletion {
    void completeApprovedApplicationForIdentity(Long identityId, Instant now);
}
