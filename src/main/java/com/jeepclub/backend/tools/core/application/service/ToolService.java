package com.jeepclub.backend.tools.core.application.service;

import com.jeepclub.backend.tools.api.dto.ToolCreateRequestDTO;
import com.jeepclub.backend.tools.api.dto.ToolUpdateRequestDTO;
import com.jeepclub.backend.tools.core.application.exception.ToolNotFoundException;
import com.jeepclub.backend.tools.core.domain.model.Tool;
import com.jeepclub.backend.tools.core.repository.ToolRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service

public class ToolService {

    private final ToolRepository toolRepository;

    // CONSTRUTOR MANUAL ADICIONADO AQUI:

    // kkk pq insiste em não usar o lombok? vc pode substituir essa linha 22 por @RequiredArgsConstructor pesquise sobre;
    public ToolService(ToolRepository toolRepository) {
        this.toolRepository = toolRepository;
    }

    public Page<Tool> listUserTools(Long userId, Pageable pageable) {
        return toolRepository.findByUserId(userId, pageable);
    }

    public Tool getToolDetails(Long toolId, Long userId) {
        Tool tool = toolRepository.findById(toolId)
                // Usando a exceção de Aplicação corretamente! boa!!
                .orElseThrow(() -> new ToolNotFoundException("Ferramenta não encontrada no banco de dados."));

        // Usando a exceção de Domínio (dentro da entidade) boa
        tool.assertBelongsTo(userId);

        return tool;
    }

    @Transactional
    public Tool createTool(ToolCreateRequestDTO request, Long userId) {
        Tool tool = Tool.create(
                request.name(),
                request.description(),
                request.status(),
                // esse status não deveria ser requisitado,
                // faça seu Tool.create definir um status padrão que as
                // ferramentas nascem. e crie rotas para alterar o status posteriormente
                userId
        );

        return toolRepository.save(tool);
    }

    @Transactional
    public Tool updateTool(Long id, ToolUpdateRequestDTO request, Long userId) {
        Tool tool = getToolDetails(id, userId);

        tool.updateDetails(request.name(), request.description());

        if (request.status() != null) {
            tool.changeStatus(request.status());
        }
        // uma rota de updade não deveria atualizar o status sem validação. o status só deve ser atualizado por meio de
        // rotas de alteração do status.

        return toolRepository.save(tool);
    }

    //esse delete deveria ser selfDelete ou seja, achar a tool, e alterar o status dela pra Deleted e atribuir o now a deletedAt
    @Transactional
    public void deleteTool(Long id, Long userId) {
        Tool tool = getToolDetails(id, userId);
        toolRepository.delete(tool);
    }
}