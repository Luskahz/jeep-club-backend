package com.jeepclub.backend.memberships.core.port;
/** Operational release gate, independent from authorization permissions. */
public interface MemberImportApplicationPolicy {
    boolean applyEnabled();
}
