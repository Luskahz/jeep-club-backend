package com.jeepclub.backend.authentication.core.repository;

import com.jeepclub.backend.authentication.core.domain.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    boolean existsById(Long id);

    List<Long> findActiveUserIds();

    boolean existsActiveById(Long id);
}