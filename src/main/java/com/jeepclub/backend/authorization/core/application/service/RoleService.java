package com.jeepclub.backend.authorization.core.application.service;


import com.jeepclub.backend.authorization.core.application.exception.RoleAlreadyExistsException;
import com.jeepclub.backend.authorization.core.application.result.role.CreateRoleResult;
import com.jeepclub.backend.authorization.core.application.result.role.FindAllRolesResult;
import com.jeepclub.backend.authorization.core.domain.model.Role;
import com.jeepclub.backend.authorization.core.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;

    @Transactional
    public CreateRoleResult createRole(String name, String description) {
        if (roleRepository.existsByName(name)) {
            throw new RoleAlreadyExistsException(name);
        }

        Role role = Role.create(name, description);

        Role savedRole = roleRepository.save(role);

        return new CreateRoleResult(savedRole);
    }

    @Transactional(readOnly = true)
    public FindAllRolesResult findAllRoles(){
        List<Role> roles = roleRepository.findAll();

        return new FindAllRolesResult(roles);
    }
}
