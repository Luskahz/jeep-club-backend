package com.jeepclub.backend.iam.authorization.core.application.service.role;

import com.jeepclub.backend.iam.authorization.core.application.exception.role.RoleAlreadyExistsException;
import com.jeepclub.backend.iam.authorization.core.application.exception.role.RoleNotFoundException;
import com.jeepclub.backend.iam.authorization.core.application.result.RoleResult;
import com.jeepclub.backend.iam.authorization.core.application.result.RolesResult;
import com.jeepclub.backend.iam.authorization.core.domain.model.Role;
import com.jeepclub.backend.iam.authorization.core.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminRoleService {

    private final RoleRepository roleRepository;
    private final Clock clock;
    private final java.util.List<com.jeepclub.backend.iam.authorization.api.module.spi.RoleDeletionGuard> deletionGuards;
    private final com.jeepclub.backend.platform.logging.SystemLogService audit;

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
        Role role = roleRepository.findByIdForUpdate(roleId)
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
        Role role = roleRepository.findByIdForUpdate(id)
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
        Role role = roleRepository.findByIdForUpdate(id)
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
    public RoleResult deleteRole(Long id, Long actorId) {
        java.util.Objects.requireNonNull(actorId, "Deletion actor is required");
        Role role = roleRepository.findByIdForUpdate(id).orElseThrow(() -> new RoleNotFoundException(id));
        // ROOT and edit restrictions are checked before dependency guards.
        if (role.isRoot()) throw new com.jeepclub.backend.iam.authorization.core.domain.exception.role.RootRoleCannotBeChangedException(id);
        if (deletionGuards.stream().anyMatch(guard -> guard.hasRetainedReferences(id))) {
            throw new com.jeepclub.backend.iam.authorization.core.domain.exception.role.RoleLifecycleConflictException(
                    "Role has retained dependencies.");
        }
        Instant now = Instant.now(clock);
        roleRepository.deleteWithHistory(id, actorId, now);
        audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(actorId,
                "AUTHORIZATION_ROLE_DELETE", "DELETE", "/authorization/roles/" + id, 204,
                com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS, 0, null, now));
        return new RoleResult(role);
    }

    @Transactional
    public RoleResult archiveRole(Long id, Long actorId) { return archiveTransition(id, actorId, true); }

    @Transactional
    public RoleResult unarchiveRole(Long id, Long actorId) { return archiveTransition(id, actorId, false); }

    private RoleResult archiveTransition(Long id, Long actorId, boolean archive) {
        java.util.Objects.requireNonNull(actorId, "Lifecycle actor is required");
        Role role = roleRepository.findByIdForUpdate(id).orElseThrow(() -> new RoleNotFoundException(id));
        Instant now = Instant.now(clock);
        if (archive ? role.archive(now) : role.unarchive(now)) {
            role = roleRepository.save(role);
            audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(actorId,
                    archive ? "AUTHORIZATION_ROLE_ARCHIVE" : "AUTHORIZATION_ROLE_UNARCHIVE", "PATCH",
                    "/authorization/roles/" + id + (archive ? "/archive" : "/unarchive"), 200,
                    com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS, 0, null, now));
        }
        return new RoleResult(role);
    }
    @Transactional
    public RoleResult activateRole(Long id,Long actorId) {
        var previous=roleRepository.findByIdForUpdate(id).orElseThrow(() -> new RoleNotFoundException(id)).getStatus();
        var result=activateRole(id);
        if(previous!=result.role().getStatus()) audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(
                actorId,"AUTHORIZATION_ROLE_ACTIVATE:{"+previous+"}","PATCH","/authorization/roles/"+id+"/activate",200,
                com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS,0,null,Instant.now(clock)));
        return result;
    }
    @Transactional
    public RoleResult deactivateRole(Long id,Long actorId) {
        var previous=roleRepository.findByIdForUpdate(id).orElseThrow(() -> new RoleNotFoundException(id)).getStatus();
        var result=deactivateRole(id);
        if(previous!=result.role().getStatus()) audit.recordInTransaction(new com.jeepclub.backend.platform.logging.SystemLogEvent(
                actorId,"AUTHORIZATION_ROLE_DEACTIVATE:{"+previous+"}","PATCH","/authorization/roles/"+id+"/deactivate",200,
                com.jeepclub.backend.platform.logging.SystemLogOutcome.SUCCESS,0,null,Instant.now(clock)));
        return result;
    }

}
