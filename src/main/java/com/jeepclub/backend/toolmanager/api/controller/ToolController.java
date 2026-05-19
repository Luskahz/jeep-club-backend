package com.jeepclub.backend.toolmanager.api.controller;

import com.jeepclub.backend.toolmanager.api.dto.ToolResponseDTO;
import com.jeepclub.backend.toolmanager.core.service.ToolQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tools")
@Tag(name = "Tool Manager - Public", description = "Consulta de ferramentas e equipamentos disponíveis.")
public class ToolController {

    private final ToolQueryService toolQueryService;

    public ToolController(ToolQueryService toolQueryService) {
        this.toolQueryService = toolQueryService;
    }

    @GetMapping
    @Operation(summary = "Listar ferramentas disponíveis", description = "Retorna todas as ferramentas que estão prontas para uso/empréstimo.")
    public ResponseEntity<List<ToolResponseDTO>> getAvailableTools() {
        List<ToolResponseDTO> tools = toolQueryService.listAvailableTools()
                .stream()
                .map(ToolResponseDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tools);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar detalhes da ferramenta", description = "Retorna os dados de uma ferramenta específica pelo ID.")
    public ResponseEntity<ToolResponseDTO> getToolById(@PathVariable Long id) {
        ToolResponseDTO tool = new ToolResponseDTO(toolQueryService.getToolDetails(id));
        return ResponseEntity.ok(tool);
    }
}