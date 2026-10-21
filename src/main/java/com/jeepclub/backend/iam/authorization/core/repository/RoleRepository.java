package com.jeepclub.backend.iam.authorization.core.repository;

import com.jeepclub.backend.iam.authorization.core.domain.model.Role;

import java.util.List;
import java.util.Optional;

public interface RoleRepository {

    List<Role> findAll();

    Optional<Role> findById(Long id);

    Optional<Role> findByIdForUpdate(Long id);

    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    Role save(Role role);

    void deleteWithHistory(Long roleId, Long actorId, java.time.Instant now);

    Optional<Role> findRoot();
    Optional<Role> findRootForUpdate();
}