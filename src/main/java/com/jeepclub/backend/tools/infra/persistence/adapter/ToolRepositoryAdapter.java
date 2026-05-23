package com.jeepclub.backend.tools.infra.persistence.adapter;

import com.jeepclub.backend.tools.core.domain.model.Tool;
import com.jeepclub.backend.tools.core.repository.ToolRepository;
import com.jeepclub.backend.tools.infra.persistence.jpa.ToolJpaRepository;
import com.jeepclub.backend.tools.infra.persistence.mapper.ToolMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
// aqui ficou ok, mas vai aumentar conforme estiver desenvolvendo as rotas.
public class ToolRepositoryAdapter implements ToolRepository {

    private final ToolJpaRepository jpaRepository;
    private final ToolMapper mapper;


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