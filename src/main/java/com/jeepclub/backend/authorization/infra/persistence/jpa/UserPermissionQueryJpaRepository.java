package com.jeepclub.backend.authorization.infra.persistence.jpa;

import com.jeepclub.backend.authorization.core.domain.enums.PermissionCode;
import com.jeepclub.backend.authorization.core.domain.enums.RoleStatus;
import com.jeepclub.backend.authorization.infra.persistence.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPermissionQueryJpaRepository extends JpaRepository<UserRoleEntity, Long> {

    @Query("""
            select distinct permission.code
            from UserRoleEntity userRole
            join RolePermissionEntity rolePermission
                on rolePermission.role.id = userRole.role.id
            join PermissionEntity permission
                on permission.id = rolePermission.permission.id
            where userRole.userId = :userId
              and userRole.role.status = :activeStatus
            order by permission.code
            """)
    List<PermissionCode> findPermissionCodesByUserId(
            Long userId,
            RoleStatus activeStatus
    );
}