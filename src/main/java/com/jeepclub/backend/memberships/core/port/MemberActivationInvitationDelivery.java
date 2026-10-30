package com.jeepclub.backend.memberships.core.port;

/** Keeps the bearer only in memory until the surrounding transaction commits. */
public interface MemberActivationInvitationDelivery {
    void afterCommit(Long applicationId, String tokenHash, String email, String name, String link);
}
