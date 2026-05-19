package com.jeepclub.backend.toolmanager.infra.persistence.adapter;

import com.jeepclub.backend.toolmanager.domain.enums.ToolStatus;
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
    public List<Tool> findAllAvailable() {
        return jpaRepository.findAllByStatus(ToolStatus.AVAILABLE)
                .stream()
                .map(mapper::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Tool> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }
}