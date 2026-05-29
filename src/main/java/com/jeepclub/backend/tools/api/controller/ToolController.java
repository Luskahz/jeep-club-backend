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
import org.springdoc.core.annotations.ParameterObject;
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
public class ToolController {

    private final ToolService toolService;

    @GetMapping
    @Operation(summary = "Listar ferramentas ativas do usuário")
    public ResponseEntity<Page<ToolSummaryResponseDTO>> getAvailableTools(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @ParameterObject Pageable pageable) {
        Page<ToolSummaryResponseDTO> tools = toolService.listUserTools(userPrincipal.getUserId(), pageable)
                .map(ToolSummaryResponseDTO::new);
        return ResponseEntity.ok(tools);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar detalhes da ferramenta")
    public ResponseEntity<ToolResponseDTO> getToolById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ToolResponseDTO tool = new ToolResponseDTO(toolService.getToolDetails(id, userPrincipal.getUserId()));
        return ResponseEntity.ok(tool);
    }

    @PostMapping
    @Operation(summary = "Criar uma ferramenta")
    public ResponseEntity<ToolResponseDTO> createTool(
            @RequestBody ToolCreateRequestDTO request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ToolResponseDTO createdTool = new ToolResponseDTO(toolService.createTool(request, userPrincipal.getUserId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTool);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar detalhes de uma ferramenta (Nome/Descrição)")
    public ResponseEntity<ToolResponseDTO> updateTool(
            @PathVariable Long id,
            @RequestBody ToolUpdateRequestDTO request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ToolResponseDTO updatedTool = new ToolResponseDTO(toolService.updateTool(id, request, userPrincipal.getUserId()));
        return ResponseEntity.ok(updatedTool);
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "Ativar ferramenta", description = "Muda o status da ferramenta para ACTIVE.")
    public ResponseEntity<ToolResponseDTO> activateTool(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ToolResponseDTO updatedTool = new ToolResponseDTO(toolService.activateTool(id, userPrincipal.getUserId()));
        return ResponseEntity.ok(updatedTool);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(summary = "Desativar ferramenta", description = "Muda o status da ferramenta para INACTIVE.")
    public ResponseEntity<ToolResponseDTO> deactivateTool(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ToolResponseDTO updatedTool = new ToolResponseDTO(toolService.deactivateTool(id, userPrincipal.getUserId()));
        return ResponseEntity.ok(updatedTool);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deletar uma ferramenta (Soft Delete)", description = "Marca a ferramenta como deletada. Ela não aparecerá mais nas listagens.")
    public ResponseEntity<Void> deleteTool(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        toolService.deleteTool(id, userPrincipal.getUserId());
        return ResponseEntity.noContent().build();
    }
}