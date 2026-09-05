package com.jeepclub.backend.billing.infra.integration.authorization;

import com.jeepclub.backend.iam.authorization.api.module.role.RoleQuery;
import com.jeepclub.backend.billing.core.port.BillingAuthorizationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class BillingAuthorizationAdapter implements BillingAuthorizationPort {

    private final RoleQuery roleQuery;

    @Override
    public List<Long> findUserIdsByRoleId(Long roleId) {
        Objects.requireNonNull(roleId, "roleId cannot be null");

        return roleQuery.findUserIdsByRoleId(roleId);
    }

    @Override
    public boolean existsActiveRoleById(Long roleId) {
        Objects.requireNonNull(roleId, "roleId cannot be null");

        return roleQuery.existsActiveRoleById(roleId);
    }
}
