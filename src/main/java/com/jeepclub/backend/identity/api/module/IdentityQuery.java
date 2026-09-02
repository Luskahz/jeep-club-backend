package com.jeepclub.backend.identity.api.module;

import java.util.List;

/**
 * Read-only contract for identity data used by other modules.
 *
 * <p>Authentication and credential state deliberately do not participate in
 * the administrative activity methods exposed here.</p>
 */
public interface IdentityQuery {

    boolean existsById(Long identityId);

    List<Long> findAdministrativelyActiveIdentityIds();

    boolean isAdministrativelyActive(Long identityId);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);
}
