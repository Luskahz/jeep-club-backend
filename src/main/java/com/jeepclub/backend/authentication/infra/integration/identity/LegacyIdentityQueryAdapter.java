package com.jeepclub.backend.authentication.infra.integration.identity;

import com.jeepclub.backend.authentication.core.domain.enums.AccountStatus;
import com.jeepclub.backend.authentication.infra.persistence.entity.UserEntity;
import com.jeepclub.backend.authentication.infra.persistence.jpa.UserJpaRepository;
import com.jeepclub.backend.identity.api.module.IdentityDetails;
import com.jeepclub.backend.identity.api.module.IdentityQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class LegacyIdentityQueryAdapter implements IdentityQuery {

    private final UserJpaRepository userJpaRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<IdentityDetails> findById(Long identityId) {
        Objects.requireNonNull(identityId, "identityId cannot be null");

        return userJpaRepository.findById(identityId).map(this::toDetails);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<IdentityDetails> findByCpf(String cpf) {
        if (cpf == null) {
            return Optional.empty();
        }

        return userJpaRepository.findByCpf(cpf).map(this::toDetails);
    }

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

    private IdentityDetails toDetails(UserEntity entity) {
        return new IdentityDetails(
                entity.getId(),
                entity.getName(),
                entity.getBirthDate(),
                entity.getEmail(),
                entity.getCpf(),
                entity.getRg(),
                entity.getPhoneNumber(),
                entity.getProfilePhotoUrl(),
                entity.getAccountStatus() == AccountStatus.ACTIVE,
                entity.getCreatedAt(),
                entity.getDisabledAt(),
                entity.getUpdatedAt()
        );
    }
}
