package com.jeepclub.backend.authorization.infra.persistence.jpa;

import com.jeepclub.backend.authorization.infra.persistence.entity.PermissionEntity;
import com.jeepclub.backend.authorization.infra.persistence.entity.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RolePermissionJpaRepository extends JpaRepository<RolePermissionEntity, Long> {

    boolean existsByRoleIdAndPermissionId(
            Long roleId,
            Long permissionId
    );

    @Query("""
            select permission
            from PermissionEntity permission
            where permission.id in (
                select rolePermission.permissionId
                from RolePermissionEntity rolePermission
                where rolePermission.roleId = :roleId
            )
            order by permission.code
            """)
    List<PermissionEntity> findPermissionsByRoleId(Long roleId);

    @Modifying
    @Query("""
            delete from RolePermissionEntity rolePermission
            where rolePermission.roleId = :roleId
            and rolePermission.permissionId = :permissionId
            """)
    void deleteByRoleIdAndPermissionId(
            Long roleId,
            Long permissionId
    );
}