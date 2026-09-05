package com.jeepclub.backend.iam.authorization.infra.integration.identity;

import com.jeepclub.backend.iam.authorization.core.port.UserIdentityPort;
import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthorizationIdentityAdapter implements UserIdentityPort {

    private final UserQuery identityQuery;

    @Override
    public boolean existsById(Long userId) {
        return identityQuery.existsById(userId);
    }
}
