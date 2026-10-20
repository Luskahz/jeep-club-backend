package com.jeepclub.backend.memberships.core.port;

/** Ephemeral bearer credential: never persist, audit or log the link. */
public record AccessLinkApproval(Long userId, String activationLink) {
    public AccessLinkApproval {
        if (userId == null || userId <= 0 || activationLink == null || activationLink.isBlank()) {
            throw new IllegalArgumentException("A provisioned identity and activation link are required.");
        }
    }
    @Override public String toString() { return "AccessLinkApproval[userId=" + userId + ", activationLink=[REDACTED]]"; }
}
