package com.jeepclub.backend.toolmanager.core.service;

import com.jeepclub.backend.toolmanager.domain.model.Tool;
import com.jeepclub.backend.toolmanager.domain.port.ToolRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ToolQueryService {

    private final ToolRepository toolRepository;

    public ToolQueryService(ToolRepository toolRepository) {
        this.toolRepository = toolRepository;
    }

    public List<Tool> listAvailableTools() {
        return toolRepository.findAllAvailable();
    }

    public Tool getToolDetails(Long id) {
        // O ideal é lançar uma ToolNotFoundException personalizada aqui se não achar (baseado na sua pasta exceptions)
        return toolRepository.findById(id).orElseThrow(() -> new RuntimeException("Tool not found"));
    }
}