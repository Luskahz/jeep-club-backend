package com.jeepclub.backend.memberships.core.port;

/** Authentication owns the credential lifecycle; Identity ACTIVE alone is insufficient. */
public interface MemberAuthenticationEligibility {
    boolean hasCompletedFirstAccess(Long identityId);

    /** Caller must hold the Identity lock. Retain the account lock until the creation transaction ends. */
    boolean lockAndHasCompletedFirstAccess(Long identityId);
}
