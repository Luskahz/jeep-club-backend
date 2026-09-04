package com.jeepclub.backend.authentication.infra.persistence.adapter;

import com.jeepclub.backend.authentication.core.application.exceptions.account.AuthenticationAccountConflictException;
import com.jeepclub.backend.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.authentication.infra.persistence.entity.AuthenticationAccountEntity;
import com.jeepclub.backend.authentication.infra.persistence.jpa.AuthenticationAccountJpaRepository;
import com.jeepclub.backend.authentication.infra.persistence.mapper.AuthenticationAccountMapper;
import com.jeepclub.backend.identity.infra.persistence.entity.UserEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityExistsException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AuthenticationAccountRepositoryAdapter
        implements AuthenticationAccountRepository {

    private final AuthenticationAccountJpaRepository jpaRepository;
    private final AuthenticationAccountMapper mapper;
    private final EntityManager entityManager;

    @Override
    public AuthenticationAccount create(AuthenticationAccount account) {
        AuthenticationAccountEntity entity = toEntity(account);
        try {
            entityManager.persist(entity);
            entityManager.flush();
            return mapper.toDomain(entity);
        } catch (EntityExistsException exception) {
            throw new AuthenticationAccountConflictException(exception);
        }
    }

    @Override
    public AuthenticationAccount save(AuthenticationAccount account) {
        AuthenticationAccountEntity entity = toEntity(account);
        return mapper.toDomain(jpaRepository.save(entity));
    }

    @Override
    public Optional<AuthenticationAccount> findByIdentityId(Long identityId) {
        return jpaRepository.findById(identityId).map(mapper::toDomain);
    }

    @Override
    public Optional<AuthenticationAccount> findByIdentityIdForUpdate(Long identityId) {
        return jpaRepository.findByIdentityIdForUpdate(identityId)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByIdentityId(Long identityId) {
        return jpaRepository.existsById(identityId);
    }

    private AuthenticationAccountEntity toEntity(AuthenticationAccount account) {
        UserEntity identity = entityManager.getReference(
                UserEntity.class,
                account.getIdentityId()
        );
        return mapper.toEntity(account, identity);
    }
}
