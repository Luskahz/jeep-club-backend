package com.jeepclub.backend.tools.infra.persistence.adapter;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import com.jeepclub.backend.tools.core.domain.exception.ToolAlreadyDeletedException;
import com.jeepclub.backend.tools.core.domain.model.Tool;
import com.jeepclub.backend.tools.core.repository.ToolRepository;
import com.jeepclub.backend.tools.infra.persistence.entity.ToolEntity;
import com.jeepclub.backend.tools.infra.persistence.entity.ToolHistoryEntity;
import com.jeepclub.backend.tools.infra.persistence.jpa.ToolHistoryJpaRepository;
import com.jeepclub.backend.tools.infra.persistence.jpa.ToolJpaRepository;
import com.jeepclub.backend.tools.infra.persistence.jpa.ToolSpecifications;
import com.jeepclub.backend.tools.infra.persistence.mapper.ToolHistoryMapper;
import com.jeepclub.backend.tools.infra.persistence.mapper.ToolMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ToolRepositoryAdapter implements ToolRepository {

    private final ToolJpaRepository jpaRepository;
    private final ToolHistoryJpaRepository historyJpaRepository;
    private final ToolMapper mapper;
    private final ToolHistoryMapper historyMapper;


    @Override
public Page<Tool> findAll(String name, ToolStatus status, Pageable pageable) {
    return jpaRepository.findAll(ToolSpecifications.withFilters(name, status), pageable)
            .map(mapper::toDomain);
}

    @Override
    public Page<Tool> findByUserId(Long userId, Pageable pageable) {
        return jpaRepository.findByUserId(userId, pageable)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Tool> findById(Long id) {
        return jpaRepository.findById(id)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Tool> findByIdAndUserId(Long toolId, Long userId) {
        return jpaRepository.findByIdAndUserId(toolId, userId)
                .map(mapper::toDomain);
    }

    @Override
    public Tool save(Tool tool) {
        // Converte do Domínio para Entidade do banco, salva, e converte de volta.
        var entity = mapper.toEntity(tool);
        var savedEntity = jpaRepository.save(entity);
        return mapper.toDomain(savedEntity);
    }

    @Override
    public void delete(
            Tool tool,
            Long deletedByUserId,
            LocalDateTime deletedAt
    ) {
        ToolEntity entity = jpaRepository
                .findByIdForUpdate(tool.getId())
                .orElseThrow(
                        () -> new ToolAlreadyDeletedException(
                                tool.getId()
                        )
                );

        ToolHistoryEntity history = historyMapper.toHistoryEntity(
                entity,
                deletedByUserId,
                deletedAt
        );

        historyJpaRepository.save(history);
        jpaRepository.delete(entity);
    }
}
