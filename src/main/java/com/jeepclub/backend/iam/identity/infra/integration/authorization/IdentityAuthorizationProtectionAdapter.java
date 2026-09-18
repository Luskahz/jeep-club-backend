package com.jeepclub.backend.iam.identity.infra.integration.authorization;

import com.jeepclub.backend.iam.authorization.api.module.role.RoleQuery;
import com.jeepclub.backend.iam.identity.core.port.UserAuthorizationProtectionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class IdentityAuthorizationProtectionAdapter implements UserAuthorizationProtectionPort {

    private final RoleQuery roleQuery;

    @Override
    public boolean hasRootRole(Long userId) {
        Objects.requireNonNull(userId, "userId cannot be null");

        return roleQuery.hasRootRole(userId);
    }
}
