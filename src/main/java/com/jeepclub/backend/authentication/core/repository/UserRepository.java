package com.jeepclub.backend.authentication.core.repository;

import com.jeepclub.backend.authentication.core.application.query.user.AdminUserFilter;
import com.jeepclub.backend.authentication.core.domain.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserRepository {

    User create(User user);

    User save(User user);

    Page<User> findAll(
            AdminUserFilter filter,
            Pageable pageable
    );

    Optional<User> findById(Long id);

    Optional<User> findByIdForUpdate(Long id);

    Optional<User> findByCpf(String cpf);

    Optional<User> findByCpfForUpdate(String cpf);

    boolean existsByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByRg(String rg);

    boolean existsById(Long id);

    List<Long> findActiveUserIds();

    boolean existsActiveById(Long id);
}