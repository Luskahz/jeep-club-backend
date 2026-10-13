package com.jeepclub.backend.memberships.api.module;

/** Public boundary for distinguishing Membership-owned first access from administrative invitations. */
public interface MembershipIdentityOwnership {
    boolean ownsIdentity(Long identityId);
}
