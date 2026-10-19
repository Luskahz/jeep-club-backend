package com.jeepclub.backend.tools.infra.integration.identity;

import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.tools.core.port.ToolOwnerQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdentityToolOwnerAdapter implements ToolOwnerQuery {
    private final com.jeepclub.backend.iam.identity.api.module.UserIdentityLock identityLock;

    private final UserQuery userQuery;

    @Override
    public boolean existsById(Long userId) {
        return userQuery.existsById(userId);
    }

    @Override
    public boolean isAdministrativelyActive(Long userId) {
        return identityLock.byId(userId).map(user -> user.administrativelyActive()).orElse(false);
    }
}
