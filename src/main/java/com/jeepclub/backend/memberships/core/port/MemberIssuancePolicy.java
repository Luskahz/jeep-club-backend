package com.jeepclub.backend.memberships.core.port;
public interface MemberIssuancePolicy {
    boolean enabled();
    long initialNumber();
    String format(long number);
    String signature();
}
