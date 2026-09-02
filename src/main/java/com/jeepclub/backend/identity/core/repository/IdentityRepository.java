package com.jeepclub.backend.identity.core.repository;

import com.jeepclub.backend.identity.core.domain.model.Identity;

import java.util.List;
import java.util.Optional;

public interface IdentityRepository {

    Identity create(Identity identity);

    Identity save(Identity identity);

    Optional<Identity> findById(Long id);

    Optional<Identity> findByIdForUpdate(Long id);

    boolean existsById(Long id);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByRg(String rg);

    boolean existsActiveById(Long id);

    List<Long> findActiveIds();
}
