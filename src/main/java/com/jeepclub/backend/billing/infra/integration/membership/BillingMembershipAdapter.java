package com.jeepclub.backend.billing.infra.integration.membership;

import com.jeepclub.backend.authentication.api.module.user.UserQuery;
import com.jeepclub.backend.billing.core.port.BillingMembershipPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class BillingMembershipAdapter implements BillingMembershipPort {

    private final UserQuery userQuery;

    @Override
    public List<Long> findActiveMemberUserIds() {
        return userQuery.findActiveUserIds();
    }

    @Override
    public boolean existsActiveMemberByUserId(Long userId) {
        Objects.requireNonNull(userId, "userId cannot be null");

        return userQuery.existsActiveUserById(userId);
    }
}