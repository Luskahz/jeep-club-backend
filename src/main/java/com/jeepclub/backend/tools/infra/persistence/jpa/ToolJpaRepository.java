package com.jeepclub.backend.tools.infra.persistence.jpa;

import com.jeepclub.backend.tools.infra.persistence.entity.ToolEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// boa, o importante aqui é entender pq o extends JpaRepository<SuaEntidade, tipoDoId> é importante pro jpa
public interface ToolJpaRepository extends JpaRepository<ToolEntity, Long> {

    // AQUI ESTÁ A MUDANÇA: Sai o List<ToolEntity>, entra o Page<ToolEntity> com Pageable
    Page<ToolEntity> findByUserId(Long userId, Pageable pageable);

    Optional<ToolEntity> findByIdAndUserId(Long id, Long userId);
}