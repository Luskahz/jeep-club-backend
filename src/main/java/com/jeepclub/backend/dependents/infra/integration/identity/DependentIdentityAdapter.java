package com.jeepclub.backend.dependents.infra.integration.identity;

import com.jeepclub.backend.dependents.core.port.DependentUserPort;
import com.jeepclub.backend.identity.api.module.IdentityQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DependentIdentityAdapter implements DependentUserPort {

    private final IdentityQuery identityQuery;

    @Override
    public boolean existsById(Long userId) {
        return identityQuery.existsById(userId);
    }

    @Override
    public boolean existsActiveById(Long userId) {
        return identityQuery.isAdministrativelyActive(userId);
    }

    @Override
    public boolean existsByCpf(String cpf) {
        return identityQuery.existsByCpf(cpf);
    }
}
