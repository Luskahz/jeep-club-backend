package com.jeepclub.backend.toolmanager.api.controller;

import com.jeepclub.backend.toolmanager.api.dto.ToolResponseDTO;
import com.jeepclub.backend.toolmanager.core.service.ToolQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tools")
@RequiredArgsConstructor
@Tag(name = "Tool Manager - Public", description = "Consulta de ferramentas e equipamentos disponíveis.")
public class ToolController {

    private final ToolQueryService toolQueryService;


    // atenção, essa rota está trazendo todas as tools do banco, se esse for o intuito isso tem que ser uma rota administrativa bloqueada
    // use o @AuthenticationPrincipal para descobrir qual usuario está fazendo esta requisição e filtrar para visualização apenas das ferramentas dele
    @GetMapping
    @Operation(summary = "Listar ferramentas disponíveis", description = "Retorna todas as ferramentas que estão prontas para uso/empréstimo.")
    public ResponseEntity<List<ToolResponseDTO>> getAvailableTools() {
        List<ToolResponseDTO> tools = toolQueryService.listAvailableTools()
                .stream()
                .map(ToolResponseDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tools);
    }
    // mesma observação: qualquer um que chamar essa rota pode ver as ferramentas de qualquer um
    @GetMapping("/{id}")
    @Operation(summary = "Buscar detalhes da ferramenta", description = "Retorna os dados de uma ferramenta específica pelo ID.")
    public ResponseEntity<ToolResponseDTO> getToolById(@PathVariable Long id) {
        ToolResponseDTO tool = new ToolResponseDTO(toolQueryService.getToolDetails(id));
        return ResponseEntity.ok(tool);
    }


    //dica, use o preAuthorization com o hasAuthoritie para bloquear a rota, caso for criar uma permission,
    //Apenas coloque string inline no seu controller com o @preAuthoritie(hasAuthoritie("permission")) posteriormente
    // implemento as permissions no shared.
}