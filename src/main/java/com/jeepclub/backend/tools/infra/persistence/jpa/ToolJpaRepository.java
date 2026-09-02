package com.jeepclub.backend.tools.infra.persistence.jpa;

import com.jeepclub.backend.tools.infra.persistence.entity.ToolEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ToolJpaRepository extends JpaRepository<ToolEntity, Long>,  JpaSpecificationExecutor<ToolEntity> {

        
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select t
            from ToolEntity t
            where t.id = :id
            """)
    Optional<ToolEntity> findByIdForUpdate(
            @Param("id") Long id
    );

    Page<ToolEntity> findByUserId(Long userId, Pageable pageable);

    Optional<ToolEntity> findByIdAndUserId(Long id, Long userId);
}
