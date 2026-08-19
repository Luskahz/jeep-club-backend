package com.jeepclub.backend.authentication.infra.integration.dependents;

import com.jeepclub.backend.authentication.api.module.user.UserQuery;
import com.jeepclub.backend.dependents.core.port.DependentUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DependentUserPortAdapter implements DependentUserPort {

    private final UserQuery userQuery;

    @Override
    public boolean existsById(Long userId) {
        return userQuery.existsById(userId);
    }

    @Override
    public boolean existsByCpf(String cpf) {
        return userQuery.existsByCpf(cpf);
    }
}
