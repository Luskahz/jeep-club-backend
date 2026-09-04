package com.jeepclub.backend.identity.api.module;

import java.util.List;
import java.util.Optional;

/**
 * Read-only contract for identity data used by other modules.
 *
 * <p>Authentication and credential state deliberately do not participate in
 * the administrative activity methods exposed here.</p>
 */
public interface UserQuery {

    Optional<UserDetails> findById(Long identityId);

    Optional<UserDetails> findByCpf(String cpf);

    boolean existsById(Long identityId);

    List<Long> findAdministrativelyActiveUserIds();

    boolean isAdministrativelyActive(Long identityId);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);
}
