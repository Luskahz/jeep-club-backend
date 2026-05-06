package com.jeepclub.backend.authorization.core.application.service;

import com.jeepclub.backend.authorization.core.application.exception.RoleAlreadyExistsException;
import com.jeepclub.backend.authorization.core.application.exception.RoleNotFoundException;
import com.jeepclub.backend.authorization.core.application.result.RolesResult;
import com.jeepclub.backend.authorization.core.application.result.role.RoleResult;
import com.jeepclub.backend.authorization.core.domain.model.Role;
import com.jeepclub.backend.authorization.core.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final Clock clock;

    @Transactional
    public RoleResult createRole(
            String name,
            String description
    ) {
        String normalizedName = Role.normalizeName(name);

        if (roleRepository.existsByName(normalizedName)) {
            throw new RoleAlreadyExistsException(normalizedName);
        }

        Instant now = Instant.now(clock);

        Role role = Role.create(
                normalizedName,
                description,
                now
        );

        Role savedRole = roleRepository.save(role);

        return new RoleResult(savedRole);
    }

    @Transactional(readOnly = true)
    public RolesResult findAllRoles() {
        List<Role> roles = roleRepository.findAll();

        return new RolesResult(roles);
    }

    @Transactional(readOnly = true)
    public RoleResult findRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException(id));

        return new RoleResult(role);
    }

    @Transactional
    public RoleResult updateRole(
            Long roleId,
            String name,
            String description
    ) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RoleNotFoundException(roleId));

        String normalizedName = Role.normalizeName(name);

        if (roleRepository.existsByNameAndIdNot(normalizedName, roleId)) {
            throw new RoleAlreadyExistsException(normalizedName);
        }

        Instant now = Instant.now(clock);

        boolean changed = role.update(
                normalizedName,
                description,
                now
        );

        if (changed) {
            role = roleRepository.save(role);
        }

        return new RoleResult(role);
    }

    @Transactional
    public RoleResult deactivateRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException(id));

        Instant now = Instant.now(clock);

        boolean changed = role.deactivate(now);

        if (!changed) {
            return new RoleResult(role);
        }

        Role deactivatedRole = roleRepository.save(role);

        return new RoleResult(deactivatedRole);
    }

    @Transactional
    public RoleResult activateRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException(id));

        Instant now = Instant.now(clock);

        boolean changed = role.activate(now);

        if (!changed) {
            return new RoleResult(role);
        }

        Role activatedRole = roleRepository.save(role);

        return new RoleResult(activatedRole);
    }

    @Transactional
    public RoleResult deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException(id));

        Instant now = Instant.now(clock);

        role.delete(now);

        Role deletedRole = roleRepository.save(role);

        return new RoleResult(deletedRole);
    }
}