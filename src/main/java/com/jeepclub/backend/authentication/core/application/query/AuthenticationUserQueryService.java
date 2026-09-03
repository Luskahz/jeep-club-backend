package com.jeepclub.backend.authentication.core.application.query;

import com.jeepclub.backend.authentication.api.module.user.UserQuery;
import com.jeepclub.backend.identity.api.module.IdentityQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
class AuthenticationUserQueryService implements UserQuery {

    private final IdentityQuery identityQuery;

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long userId) {
        return identityQuery.existsById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findActiveUserIds() {
        return identityQuery.findAdministrativelyActiveIdentityIds();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsActiveUserById(Long userId) {
        Objects.requireNonNull(userId, "userId cannot be null");

        return identityQuery.isAdministrativelyActive(userId);
    }

    @Override
    public boolean existsByCpf(String cpf) {
        return identityQuery.existsByCpf(cpf);
    }

    @Override
    public boolean existsByEmail(String email) {
        return identityQuery.existsByEmail(email);
    }
}
