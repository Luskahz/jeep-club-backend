package com.jeepclub.backend.memberships.core.port;
public interface MemberIdentityPort {
    boolean exists(Long identityId);
    boolean isActive(Long identityId);
    boolean lockAndIsActive(Long identityId);
}
