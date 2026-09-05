package com.jeepclub.backend.iam.authorization.infra.persistence.adapter;

import com.jeepclub.backend.iam.authorization.core.domain.model.Role;
import com.jeepclub.backend.iam.authorization.core.domain.model.UserRole;
import com.jeepclub.backend.iam.authorization.core.repository.UserRoleRepository;
import com.jeepclub.backend.iam.authorization.infra.persistence.entity.UserRoleEntity;
import com.jeepclub.backend.iam.authorization.infra.persistence.jpa.UserRoleJpaRepository;
import com.jeepclub.backend.iam.authorization.infra.persistence.mapper.RoleMapper;
import com.jeepclub.backend.iam.authorization.infra.persistence.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserRoleRepositoryAdapter implements UserRoleRepository {

    private final UserRoleJpaRepository jpa;
    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    @Override
    public UserRole save(UserRole userRole) {
        UserRoleEntity entity = userRoleMapper.toEntity(userRole);

        UserRoleEntity savedEntity = jpa.save(entity);

        return userRoleMapper.toDomain(savedEntity);
    }

    @Override
    public List<UserRole> saveAll(List<UserRole> userRoles) {
        List<UserRoleEntity> entities = userRoles.stream()
                .map(userRoleMapper::toEntity)
                .toList();

        return jpa.saveAll(entities)
                .stream()
                .map(userRoleMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByUserIdAndRoleId(
            Long userId,
            Long roleId
    ) {
        return jpa.existsByUserIdAndRole_Id(userId, roleId);
    }

    @Override
    public List<Role> findRolesByUserId(Long userId) {
        return jpa.findRolesByUserId(userId)
                .stream()
                .map(roleMapper::toDomain)
                .toList();
    }

    @Override
    public List<Long> findUserIdsByRoleId(Long roleId) {
        return jpa.findUserIdsByRoleId(roleId);
    }

    @Override
    public void deleteByUserIdAndRoleId(
            Long userId,
            Long roleId
    ) {
        jpa.deleteByUserIdAndRole_Id(userId, roleId);
    }

    @Override
    public void deleteByUserId(Long userId) {
        jpa.deleteByUserId(userId);
    }
}