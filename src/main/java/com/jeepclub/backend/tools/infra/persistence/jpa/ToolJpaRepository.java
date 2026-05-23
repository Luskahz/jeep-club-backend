package com.jeepclub.backend.tools.infra.persistence.jpa;

import com.jeepclub.backend.tools.infra.persistence.entity.ToolEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ToolJpaRepository extends JpaRepository<ToolEntity, Long> {
    // Apague o findAllByStatus antigo e coloque estes dois:
    List<ToolEntity> findAllByUserId(Long userId);
    Optional<ToolEntity> findByIdAndUserId(Long id, Long userId);
}