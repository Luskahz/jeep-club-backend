package com.jeepclub.backend.authorization.infra.persistence.jpa;

import com.jeepclub.backend.authorization.infra.persistence.entity.RoleEntity;
import com.jeepclub.backend.authorization.infra.persistence.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserRoleJpaRepository extends JpaRepository<UserRoleEntity, Long> {

    boolean existsByUserIdAndRoleId(
            Long userId,
            Long roleId
    );

    @Query("""
            select role
            from RoleEntity role
            where role.id in (
                select userRole.roleId
                from UserRoleEntity userRole
                where userRole.userId = :userId
            )
            order by role.name
            """)
    List<RoleEntity> findRolesByUserId(Long userId);

    @Modifying
    @Query("""
            delete from UserRoleEntity userRole
            where userRole.userId = :userId
            and userRole.roleId = :roleId
            """)
    void deleteByUserIdAndRoleId(
            Long userId,
            Long roleId
    );

    @Modifying
    @Query("""
            delete from UserRoleEntity userRole
            where userRole.userId = :userId
            """)
    void deleteByUserId(Long userId);
}