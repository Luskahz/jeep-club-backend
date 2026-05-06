package com.jeepclub.backend.authorization.infra.persistence.jpa;

import com.jeepclub.backend.authorization.infra.persistence.entity.PermissionEntity;
import com.jeepclub.backend.authorization.infra.persistence.entity.RolePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RolePermissionJpaRepository extends JpaRepository<RolePermissionEntity, Long> {

    boolean existsByRole_IdAndPermission_Id(Long roleId, Long permissionId);

    void deleteByRole_IdAndPermission_Id(Long roleId, Long permissionId);

    void deleteByRole_Id(Long roleId);

    @Query("""
        select rolePermission.permission
        from RolePermissionEntity rolePermission
        where rolePermission.role.id = :roleId
        order by rolePermission.permission.code
    """)
    List<PermissionEntity> findPermissionsByRoleId(@Param("roleId") Long roleId);
}