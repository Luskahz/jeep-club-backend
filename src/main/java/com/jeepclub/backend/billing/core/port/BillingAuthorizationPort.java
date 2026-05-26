package com.jeepclub.backend.billing.core.port;

import java.util.List;

public interface BillingAuthorizationPort {

    List<Long> findUserIdsByRoleId(Long roleId);
}