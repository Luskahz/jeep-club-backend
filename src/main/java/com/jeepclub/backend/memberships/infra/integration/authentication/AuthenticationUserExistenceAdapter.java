package com.jeepclub.backend.memberships.infra.integration.authentication;

import com.jeepclub.backend.authentication.api.module.user.UserQuery;
import com.jeepclub.backend.memberships.core.port.UserExistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthenticationUserExistenceAdapter implements UserExistencePort {

    private final UserQuery userQuery;

    @Override
    public boolean existsByCpf(String cpf) {
        return userQuery.existsByCpf(cpf);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userQuery.existsByEmail(email);
    }
}
