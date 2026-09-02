package com.jeepclub.backend.authentication.core.repository;

import com.jeepclub.backend.authentication.core.domain.model.AuthenticationAccount;

import java.util.Optional;

public interface AuthenticationAccountRepository {

    AuthenticationAccount create(AuthenticationAccount account);

    AuthenticationAccount save(AuthenticationAccount account);

    Optional<AuthenticationAccount> findByIdentityId(Long identityId);

    Optional<AuthenticationAccount> findByIdentityIdForUpdate(Long identityId);

    boolean existsByIdentityId(Long identityId);
}
