package com.jeepclub.backend.billing.infra.integration.identity;

import com.jeepclub.backend.billing.core.port.BillingMembershipPort;
import com.jeepclub.backend.identity.api.module.UserQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class IdentityBillingMembershipAdapter implements BillingMembershipPort {

    private final UserQuery identityQuery;

    @Override
    public List<Long> findActiveMemberUserIds() {
        return identityQuery.findAdministrativelyActiveUserIds();
    }

    @Override
    public boolean existsActiveMemberByUserId(Long userId) {
        Objects.requireNonNull(userId, "userId cannot be null");

        return identityQuery.isAdministrativelyActive(userId);
    }
}
