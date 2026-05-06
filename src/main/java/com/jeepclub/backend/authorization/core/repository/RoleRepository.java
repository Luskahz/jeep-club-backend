package com.jeepclub.backend.authorization.core.repository;

import com.jeepclub.backend.authorization.core.domain.model.Role;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface RoleRepository {

    List<Role> findAll();

    Optional<Role> findById(Long id);

    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    Role save(Role role);
}