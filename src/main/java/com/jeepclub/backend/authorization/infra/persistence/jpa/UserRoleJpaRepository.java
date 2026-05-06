package com.jeepclub.backend.authorization.infra.persistence.jpa;

import com.jeepclub.backend.authorization.infra.persistence.entity.RoleEntity;
import com.jeepclub.backend.authorization.infra.persistence.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRoleJpaRepository extends JpaRepository<UserRoleEntity, Long> {

    boolean existsByUserIdAndRole_Id(Long userId, Long roleId);

    @Query("""
        select userRole.role
        from UserRoleEntity userRole
        where userRole.userId = :userId
        order by userRole.role.name
    """)
    List<RoleEntity> findRolesByUserId(@Param("userId") Long userId);

    void deleteByUserIdAndRole_Id(Long userId, Long roleId);

    void deleteByUserId(Long userId);
}