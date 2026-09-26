package com.jeepclub.backend.memberships.api.module;

public interface MembershipAccessQuery {

    MembershipAccessResult evaluate(Long userId);
}
