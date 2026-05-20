package com.jeepclub.backend.toolmanager.infra.persistence.adapter;

import com.jeepclub.backend.toolmanager.domain.model.Tool;
import com.jeepclub.backend.toolmanager.domain.port.ToolRepository;
import com.jeepclub.backend.toolmanager.infra.persistence.jpa.ToolJpaRepository;
import com.jeepclub.backend.toolmanager.infra.persistence.mapper.ToolMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ToolRepositoryAdapter implements ToolRepository {

    private final ToolJpaRepository jpaRepository;
    private final ToolMapper mapper;

    public ToolRepositoryAdapter(ToolJpaRepository jpaRepository, ToolMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public List<Tool> findAllByUserId(Long userId) {
        return jpaRepository.findAllByUserId(userId)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Tool> findByIdAndUserId(Long toolId, Long userId) {
        return jpaRepository.findByIdAndUserId(toolId, userId)
                .map(mapper::toDomain);
    }
}