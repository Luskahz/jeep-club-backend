package com.jeepclub.backend.iam.identity.api.module;

import java.util.Optional;

/** Transactional read boundary. Locks Identity before Authentication/activation records. */
public interface UserIdentityLock {
    Optional<UserDetails> byId(Long userId);
    Optional<UserDetails> byCpf(String cpf);
}
