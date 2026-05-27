package com.jeepclub.backend.tools.api.controller;

import com.jeepclub.backend.infra.security.principal.UserPrincipal;
import com.jeepclub.backend.tools.api.dto.ToolCreateRequestDTO;
import com.jeepclub.backend.tools.api.dto.ToolResponseDTO;
import com.jeepclub.backend.tools.api.dto.ToolSummaryResponseDTO;
import com.jeepclub.backend.tools.api.dto.ToolUpdateRequestDTO;
import com.jeepclub.backend.tools.core.application.service.ToolService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject; // <-- IMPORT MÁGICO ADICIONADO AQUI
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tools")
@RequiredArgsConstructor
@Tag(name = "Tools", description = "Gerenciamento de ferramentas e equipamentos do usuário.")

// crie uma rota para alteração do status da ferramenta, o frontend não deve ser obrigado a estudar os status internos do backend
// e sim saber quais rotas alteram os status.

public class ToolController {

    private final ToolService toolService;

    // 1. Rota GET /tools - Agora com Paginação e DTO Resumido
    @GetMapping
    @Operation(summary = "Listar ferramentas do usuário", description = "Retorna uma lista paginada e resumida das ferramentas pertencentes ao usuário logado.")
    public ResponseEntity<Page<ToolSummaryResponseDTO>> getAvailableTools(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @ParameterObject Pageable pageable) { // <-- ANOTAÇÃO APLICADA AQUI

        // O service agora deve retornar um Page em vez de List
        Page<ToolSummaryResponseDTO> tools = toolService.listUserTools(userPrincipal.getUserId(), pageable)
                .map(ToolSummaryResponseDTO::new);

        return ResponseEntity.ok(tools);
    }

    // 2. Rota GET /tools/{id} - Mantida retornando todos os dados
    @GetMapping("/{id}")
    @Operation(summary = "Buscar detalhes da ferramenta", description = "Retorna os dados completos de uma ferramenta específica do usuário.")
    public ResponseEntity<ToolResponseDTO> getToolById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        ToolResponseDTO tool = new ToolResponseDTO(toolService.getToolDetails(id, userPrincipal.getUserId()));
        return ResponseEntity.ok(tool);
    }

    // 3. Rota POST /tools - Nova rota para Criar ferramenta
    @PostMapping
    @Operation(summary = "Criar uma ferramenta", description = "Cadastra uma nova ferramenta no inventário do usuário logado.")
    public ResponseEntity<ToolResponseDTO> createTool(
            @RequestBody ToolCreateRequestDTO request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        ToolResponseDTO createdTool = new ToolResponseDTO(toolService.createTool(request, userPrincipal.getUserId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTool);
    }

    // 4. Rota PUT /tools/{id} - Nova rota para Editar ferramenta
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar uma ferramenta", description = "Atualiza os dados de uma ferramenta existente do usuário logado.")
    public ResponseEntity<ToolResponseDTO> updateTool(
            @PathVariable Long id,
            @RequestBody ToolUpdateRequestDTO request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        ToolResponseDTO updatedTool = new ToolResponseDTO(toolService.updateTool(id, request, userPrincipal.getUserId()));
        return ResponseEntity.ok(updatedTool);
    }

    // 5. Rota DELETE /tools/{id} - Nova rota para SelfDelete

    // essa rota que vc fez não é selfDelete, ela é um delete cru, uma rota set delete deve definir o status da
    // ferramenta para DELETED a ferramenta deve ter um campo deletedAt e as buscas não devem ter mais esta ferramenta,
    // por mais que ela ainda esteja no banco.
    // sera necessario criar filtros no service para busca para trazer apenas ferramentas que estão ativas, avalie os status que criou
    // coloquei comentarios nos enums também, validar todos os arquivos.
    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar uma ferramenta", description = "Remove permanentemente uma ferramenta do usuário logado.")
    public ResponseEntity<Void> deleteTool(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {

        toolService.deleteTool(id, userPrincipal.getUserId());
        return ResponseEntity.noContent().build(); // Retorna 204 No Content
    }
}