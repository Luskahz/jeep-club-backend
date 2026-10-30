package com.jeepclub.backend.memberships.core.port;

import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;

/** Persist a pending notice in the surrounding rejection transaction. */
public interface MembershipRejectionNotifier {
    void enqueue(MembershipApplication rejectedApplication);
}
