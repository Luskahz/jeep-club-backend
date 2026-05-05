package com.jeepclub.backend.authorization.core.repository;

import com.jeepclub.backend.authorization.core.domain.enums.PermissionCode;
import com.jeepclub.backend.authorization.core.domain.model.Permission;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository {

    List<Permission> findAll();

    Optional<Permission> findById(Long id);

    Optional<Permission> findByCode(PermissionCode code);

    Permission save(Permission permission);
}
