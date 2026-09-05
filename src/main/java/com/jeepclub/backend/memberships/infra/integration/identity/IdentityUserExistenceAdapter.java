package com.jeepclub.backend.memberships.infra.integration.identity;

import com.jeepclub.backend.iam.identity.api.module.UserQuery;
import com.jeepclub.backend.memberships.core.port.UserExistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class IdentityUserExistenceAdapter implements UserExistencePort {

    private final UserQuery identityQuery;

    @Override
    public boolean existsByCpf(String cpf) {
        return identityQuery.existsByCpf(cpf);
    }

    @Override
    public boolean existsByEmail(String email) {
        return identityQuery.existsByEmail(email);
    }
}
