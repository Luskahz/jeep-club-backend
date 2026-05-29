package com.jeepclub.backend.tools.infra.persistence.adapter;

import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import com.jeepclub.backend.tools.core.domain.model.Tool;
import com.jeepclub.backend.tools.core.repository.ToolRepository;
import com.jeepclub.backend.tools.infra.persistence.jpa.ToolJpaRepository;
import com.jeepclub.backend.tools.infra.persistence.mapper.ToolMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ToolRepositoryAdapter implements ToolRepository {

    private final ToolJpaRepository jpaRepository;
    private final ToolMapper mapper;

    @Override
    public Page<Tool> findByUserId(Long userId, Pageable pageable) {
        // Chamando o método NOVO com "AndStatusNot"
        return jpaRepository.findByUserIdAndStatusNot(userId, ToolStatus.DELETED, pageable)
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
    public void delete(Tool tool) {
        var entity = mapper.toEntity(tool);
        jpaRepository.delete(entity);
    }
}