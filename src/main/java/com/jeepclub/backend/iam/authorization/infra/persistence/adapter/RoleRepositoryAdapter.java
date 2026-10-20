package com.jeepclub.backend.iam.authorization.infra.persistence.adapter;

import com.jeepclub.backend.iam.authorization.core.domain.enums.RoleKind;
import com.jeepclub.backend.iam.authorization.core.domain.model.Role;
import com.jeepclub.backend.iam.authorization.core.repository.RoleRepository;
import com.jeepclub.backend.iam.authorization.infra.persistence.entity.RoleEntity;
import com.jeepclub.backend.iam.authorization.infra.persistence.jpa.RoleJpaRepository;
import com.jeepclub.backend.iam.authorization.infra.persistence.mapper.RoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RoleRepositoryAdapter implements RoleRepository {
    private final jakarta.persistence.EntityManager entityManager;


    private final RoleJpaRepository jpa;
    private final com.jeepclub.backend.iam.authorization.infra.persistence.jpa.RolePermissionJpaRepository rolePermissions;
    private final com.jeepclub.backend.iam.authorization.infra.persistence.jpa.UserRoleJpaRepository userRoles;
    private final com.jeepclub.backend.iam.authorization.infra.persistence.jpa.RoleDeletionHistoryJpaRepository deletionHistory;
    private final com.jeepclub.backend.iam.authorization.infra.persistence.jpa.RolePermissionDeletionHistoryJpaRepository permissionHistory;
    private final RoleMapper mapper;

    @Override
    public List<Role> findAll() {
        return jpa.findAll()
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Role> findById(Long id) {
        return jpa.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Role> findByName(String name) {
        return jpa.findByName(name)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByName(String name) {
        return jpa.existsByName(name);
    }

    @Override
    public boolean existsByNameAndIdNot(String name, Long id) {
        return jpa.existsByNameAndIdNot(name, id);
    }

    @Override
    public Role save(Role role) {
        RoleEntity entity = mapper.toEntity(role);

        RoleEntity savedEntity = jpa.save(entity);

        return mapper.toDomain(savedEntity);
    }

    @Override
    public Optional<Role> findRoot() {
        return jpa.findByKind(RoleKind.ROOT)
                .map(mapper::toDomain);
    }
    @Override
    public Optional<Role> findByIdForUpdate(Long id) {
        var locked = jpa.findByIdForUpdate(id);
        // Retain the DB lock while discarding any pre-lock ORM snapshot (MySQL REPEATABLE READ).
        locked.ifPresent(entityManager::detach);
        return locked.flatMap(ignored -> jpa.findByIdForUpdate(id)).map(mapper::toDomain);
    }

    @Override
    public void deleteWithHistory(Long roleId, Long actorId, java.time.Instant now) {
        java.util.Objects.requireNonNull(actorId, "Deletion actor is required");
        var current = findByIdForUpdate(roleId).orElseThrow(() ->
                new com.jeepclub.backend.iam.authorization.core.application.exception.role.RoleNotFoundException(roleId));
        current.ensureCanBeDeleted();
        if (!userRoles.findByRole_IdOrderById(roleId).isEmpty()) {
            throw new com.jeepclub.backend.iam.authorization.core.domain.exception.role.RoleLifecycleConflictException(
                    "Role still has assigned users.");
        }
        var links = rolePermissions.findByRole_IdOrderById(roleId);
        deletionHistory.saveAndFlush(new com.jeepclub.backend.iam.authorization.infra.persistence.entity.RoleDeletionHistoryEntity(
                current.getId(), current.getName(), current.getDescription(), current.getKind().name(),
                current.getStatus().name(), current.getCreatedAt(), current.getUpdatedAt(), current.getArchivedAt(),
                actorId, now, "ADMINISTRATIVE_DELETE", false, 1));
        permissionHistory.saveAll(links.stream().map(link ->
                new com.jeepclub.backend.iam.authorization.infra.persistence.entity.RolePermissionDeletionHistoryEntity(
                        link.getId(), roleId, link.getPermission().getId(), link.getPermission().getCode().name(),
                        link.getCreatedAt())).toList());
        // Remove managed associations as entities; a bulk DELETE leaves stale references
        // in the persistence context and fails Hibernate's parent-removal validation.
        rolePermissions.deleteAll(links);
        rolePermissions.flush();
        jpa.deleteById(roleId);
        jpa.flush();
    }

    @Override public Optional<Role> findRootForUpdate() {
        var locked = jpa.findLockedByKind(RoleKind.ROOT);
        locked.ifPresent(entityManager::detach);
        return locked.flatMap(ignored -> jpa.findLockedByKind(RoleKind.ROOT)).map(mapper::toDomain);
    }
}
