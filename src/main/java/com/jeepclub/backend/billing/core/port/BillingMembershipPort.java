package com.jeepclub.backend.billing.core.port;

import java.util.List;

public interface BillingMembershipPort {

    List<Long> findActiveMemberUserIds();

    boolean existsActiveMemberByUserId(Long userId);
}