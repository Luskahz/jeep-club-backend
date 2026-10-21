package com.jeepclub.backend.iam.authentication.infra.persistence.adapter;

import com.jeepclub.backend.iam.authentication.core.application.exceptions.account.AuthenticationAccountConflictException;
import com.jeepclub.backend.iam.authentication.core.domain.model.AuthenticationAccount;
import com.jeepclub.backend.iam.authentication.core.repository.AuthenticationAccountRepository;
import com.jeepclub.backend.iam.authentication.infra.persistence.entity.AuthenticationAccountEntity;
import com.jeepclub.backend.iam.authentication.infra.persistence.jpa.AuthenticationAccountJpaRepository;
import com.jeepclub.backend.iam.authentication.infra.persistence.mapper.AuthenticationAccountMapper;
import com.jeepclub.backend.iam.identity.infra.persistence.entity.UserEntity;
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
        var locked = jpaRepository.findByIdentityIdForUpdate(identityId);
        locked.ifPresent(entityManager::detach);
        return locked.flatMap(ignored -> jpaRepository.findByIdentityIdForUpdate(identityId))
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByIdentityId(Long identityId) {
        return jpaRepository.existsById(identityId);
    }

    @Override
    public boolean hasSessionHistoryForUpdate(Long identityId) {
        return !entityManager.createNativeQuery("SELECT id FROM authentication_sessions WHERE user_id = :id LIMIT 1 FOR UPDATE")
                .setParameter("id", identityId).getResultList().isEmpty();
    }

    @Override
    public void deletePending(Long identityId) {
        jpaRepository.deleteById(identityId);
        jpaRepository.flush();
    }

    private AuthenticationAccountEntity toEntity(AuthenticationAccount account) {
        UserEntity identity = entityManager.getReference(
                UserEntity.class,
                account.getIdentityId()
        );
        return mapper.toEntity(account, identity);
    }
}
