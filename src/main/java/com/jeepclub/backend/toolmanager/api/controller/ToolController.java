package com.jeepclub.backend.toolmanager.api.controller;

import com.jeepclub.backend.infra.security.principal.UserPrincipal;
import com.jeepclub.backend.toolmanager.api.dto.ToolResponseDTO;
import com.jeepclub.backend.toolmanager.core.service.ToolQueryService;
// Importe a classe que representa o seu usuário logado (pela sua estrutura, deve ser essa abaixo ou JwtAuthenticatedUser)

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/tools")
@RequiredArgsConstructor // Adicionado pelo Luskahz: cria o construtor automaticamente
@Tag(name = "Tool Manager - Public", description = "Consulta de ferramentas e equipamentos do usuário.")
public class ToolController {

    private final ToolQueryService toolQueryService;

    @GetMapping
    @PreAuthorize("hasAuthority('TOOL_READ')") // Bloqueio inline solicitado pelo Luskahz
    @Operation(summary = "Listar ferramentas do usuário", description = "Retorna todas as ferramentas pertencentes ao usuário logado.")
    public ResponseEntity<List<ToolResponseDTO>> getAvailableTools(
            @AuthenticationPrincipal UserPrincipal userPrincipal) { // Captura quem está fazendo a requisição

        // Agora passamos o ID do usuário para o service buscar APENAS as ferramentas dele
        List<ToolResponseDTO> tools = toolQueryService.listUserTools(userPrincipal.getUserId())
                .stream()
                .map(ToolResponseDTO::new)
                .collect(Collectors.toList());

        return ResponseEntity.ok(tools);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TOOL_READ')") // Bloqueio inline solicitado pelo Luskahz
    @Operation(summary = "Buscar detalhes da ferramenta", description = "Retorna os dados de uma ferramenta específica do usuário.")
    public ResponseEntity<ToolResponseDTO> getToolById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) { // Captura quem está fazendo a requisição

        // Passamos o ID da ferramenta E o ID do usuário logado para garantir que a ferramenta pertence a ele
        ToolResponseDTO tool = new ToolResponseDTO(toolQueryService.getToolDetails(id, userPrincipal.getUserId()));

        return ResponseEntity.ok(tool);
    }
}