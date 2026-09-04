package com.jeepclub.backend.iam.identity.core.repository;

import com.jeepclub.backend.iam.identity.core.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User create(User user);

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByCpf(String cpf);

    Optional<User> findByIdForUpdate(Long id);

    boolean existsById(Long id);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByRg(String rg);

    boolean existsActiveById(Long id);

    List<Long> findActiveIds();
}
