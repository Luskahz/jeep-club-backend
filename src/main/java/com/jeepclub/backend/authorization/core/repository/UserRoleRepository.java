package com.jeepclub.backend.authorization.core.repository;

import com.jeepclub.backend.authorization.core.domain.model.Role;
import com.jeepclub.backend.authorization.core.domain.model.UserRole;

import java.util.List;

public interface UserRoleRepository {

    UserRole save(UserRole userRole);

    List<UserRole> saveAll(List<UserRole> userRoles);

    boolean existsByUserIdAndRoleId(Long userId, Long roleId);

    List<Role> findRolesByUserId(Long userId);

    void deleteByUserIdAndRoleId(Long userId, Long roleId);

    void deleteByUserId(Long userId);
}