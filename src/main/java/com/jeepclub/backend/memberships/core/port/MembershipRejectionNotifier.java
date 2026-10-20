package com.jeepclub.backend.memberships.core.port;

/** Schedule a notice only after the surrounding business transaction commits. */
public interface MembershipRejectionNotifier {
    void afterCommit(String email, String name, String reason);
}
