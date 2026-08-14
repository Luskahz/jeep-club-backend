package com.jeepclub.backend.authorization.infra.integration.authentication;

import com.jeepclub.backend.authentication.api.module.user.UserQuery;
import com.jeepclub.backend.authorization.core.port.UserIdentityPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationUserIdentityAdapter implements UserIdentityPort {

    private final UserQuery userQuery;

    @Override
    public boolean existsById(Long userId) {
        return userQuery.existsById(userId);
    }
}
