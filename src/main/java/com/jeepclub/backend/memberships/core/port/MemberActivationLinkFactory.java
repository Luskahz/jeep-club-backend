package com.jeepclub.backend.memberships.core.port;

public interface MemberActivationLinkFactory {
    String create(String rawToken);
}
