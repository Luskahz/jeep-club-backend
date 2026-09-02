package com.jeepclub.backend.authentication.infra.integration.identity;

import com.jeepclub.backend.authentication.core.domain.enums.AccountStatus;
import com.jeepclub.backend.authentication.infra.persistence.jpa.UserJpaRepository;
import com.jeepclub.backend.identity.api.module.IdentityQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class LegacyIdentityQueryAdapter implements IdentityQuery {

    private final UserJpaRepository userJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long identityId) {
        Objects.requireNonNull(identityId, "identityId cannot be null");

        return userJpaRepository.existsById(identityId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> findAdministrativelyActiveIdentityIds() {
        return userJpaRepository.findIdsByAccountStatus(AccountStatus.ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isAdministrativelyActive(Long identityId) {
        Objects.requireNonNull(identityId, "identityId cannot be null");

        return userJpaRepository.existsByIdAndAccountStatus(
                identityId,
                AccountStatus.ACTIVE
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCpf(String cpf) {
        return cpf != null && userJpaRepository.existsByCpf(cpf);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return email != null && userJpaRepository.existsByEmail(email);
    }
}
