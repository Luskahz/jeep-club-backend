package com.jeepclub.backend.tools.api.http.controller.admin;

import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.tools.api.http.dto.*;
import com.jeepclub.backend.tools.core.application.service.tool.AdminToolService;
import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/tools")
@RequiredArgsConstructor
@Tag(name = "Tools - Admin", description = "Gerenciamento administrativo de ferramentas de todos os usuários.")
public class AdminToolController {

    private final AdminToolService adminToolService;

    @GetMapping
    @PreAuthorize("hasAuthority('TOOLS_TOOL_READ')")
    @Operation(summary = "Lista ferramentas de todos os usuários, com busca por nome e filtro de status")
    public ResponseEntity<Page<AdminToolSummaryResponseDTO>> listTools(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) ToolStatus status,
            @ParameterObject Pageable pageable) {
        Page<AdminToolSummaryResponseDTO> tools = adminToolService
                .listAllTools(name, status, pageable)
                .map(AdminToolSummaryResponseDTO::new);
        return ResponseEntity.ok(tools);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TOOLS_TOOL_READ')")
    @Operation(summary = "Busca detalhes de uma ferramenta de qualquer usuário")
    public ResponseEntity<ToolResponseDTO> getToolById(@PathVariable Long id) {
        return ResponseEntity.ok(new ToolResponseDTO(adminToolService.getToolDetails(id)));
    }

    @PostMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('TOOLS_TOOL_CREATE')")
    @Operation(summary = "Cadastra uma ferramenta em nome de um usuário específico")
    public ResponseEntity<ToolResponseDTO> createToolForUser(
            @PathVariable Long userId,
            @RequestBody ToolCreateRequestDTO request) {
        var tool = adminToolService.createToolForUser(userId, request.name(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ToolResponseDTO(tool));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TOOLS_TOOL_UPDATE')")
    @Operation(summary = "Atualiza nome/descrição de uma ferramenta de qualquer usuário")
    public ResponseEntity<ToolResponseDTO> updateTool(
            @PathVariable Long id,
            @RequestBody ToolUpdateRequestDTO request) {
        var tool = adminToolService.updateTool(id, request.name(), request.description());
        return ResponseEntity.ok(new ToolResponseDTO(tool));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('TOOLS_TOOL_ACTIVATE')")
    @Operation(summary = "Ativa a ferramenta de qualquer usuário")
    public ResponseEntity<ToolResponseDTO> activateTool(@PathVariable Long id) {
        return ResponseEntity.ok(new ToolResponseDTO(adminToolService.activateTool(id)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('TOOLS_TOOL_DEACTIVATE')")
    @Operation(summary = "Desativa a ferramenta de qualquer usuário")
    public ResponseEntity<ToolResponseDTO> deactivateTool(@PathVariable Long id) {
        return ResponseEntity.ok(new ToolResponseDTO(adminToolService.deactivateTool(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TOOLS_TOOL_DELETE')")
    @Operation(summary = "Exclui a ferramenta de qualquer usuário")
    public ResponseEntity<Void> deleteTool(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        adminToolService.deleteTool(id, userPrincipal.getUserId());
        return ResponseEntity.noContent().build();
    }
}