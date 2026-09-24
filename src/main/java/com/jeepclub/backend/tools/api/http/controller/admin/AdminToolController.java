package com.jeepclub.backend.tools.api.http.controller.admin;

import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.web.pagination.PageResponse;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.tools.api.http.dto.*;
import com.jeepclub.backend.tools.core.application.service.tool.AdminToolService;
import com.jeepclub.backend.tools.core.domain.enums.ToolStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
    @RequiredPermission("TOOLS_TOOL_READ")
    @Operation(
            summary = "Listar ferramentas administrativamente",
            description = "Retorna uma página de ferramentas ACTIVE e INACTIVE. name aplica contains case-insensitive quando não vazio; status aplica igualdade exata; ambos podem ser combinados. A paginação usa os defaults globais: página zero-based e size 20, limitado a 50.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Página de ferramentas retornada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AdminToolSummaryPageResponseSchema.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão TOOLS_TOOL_READ.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<PageResponse<AdminToolSummaryResponseDTO>> listTools(
            @Parameter(description = "Trecho de nome para busca case-insensitive. Valor nulo ou vazio não filtra.", example = "macaco")
            @RequestParam(required = false) String name,
            @Parameter(description = "Status exato da ferramenta.", schema = @Schema(allowableValues = {"ACTIVE", "INACTIVE"}), example = "ACTIVE")
            @RequestParam(required = false) ToolStatus status,
            @ParameterObject Pageable pageable) {
        PageResponse<AdminToolSummaryResponseDTO> tools = PageResponse.from(adminToolService
                .listAllTools(name, status, pageable)
                .map(AdminToolSummaryResponseDTO::new));
        return ResponseEntity.ok(tools);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TOOLS_TOOL_READ')")
    @RequiredPermission("TOOLS_TOOL_READ")
    @Operation(
            summary = "Consultar ferramenta administrativamente",
            description = "Retorna uma ferramenta ACTIVE ou INACTIVE de qualquer usuário.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ferramenta retornada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ToolResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão TOOLS_TOOL_READ.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Ferramenta não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<ToolResponseDTO> getToolById(
            @Parameter(description = "Identificador da ferramenta.", example = "42", required = true)
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(new ToolResponseDTO(adminToolService.getToolDetails(id)));
    }

    @PostMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('TOOLS_TOOL_CREATE')")
    @RequiredPermission("TOOLS_TOOL_CREATE")
    @Operation(
            summary = "Criar ferramenta para um userId",
            description = "Cria uma ferramenta ACTIVE para um usuário existente e administrativamente ativo. Usuários DISABLED não podem receber novas ferramentas.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Ferramenta criada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ToolResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Payload inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão TOOLS_TOOL_CREATE.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Proprietário não encontrado (TOOL_OWNER_NOT_FOUND).", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Proprietário desabilitado (TOOL_OWNER_DISABLED).", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<ToolResponseDTO> createToolForUser(
            @Parameter(description = "Identificador do proprietário existente e administrativamente ativo.", example = "7", required = true)
            @PathVariable Long userId,
            @Valid @RequestBody ToolCreateRequestDTO request) {
        var tool = adminToolService.createToolForUser(userId, request.name(), request.description());
        return ResponseEntity.status(HttpStatus.CREATED).body(new ToolResponseDTO(tool));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TOOLS_TOOL_UPDATE')")
    @RequiredPermission("TOOLS_TOOL_UPDATE")
    @Operation(
            summary = "Atualizar parcialmente uma ferramenta administrativamente",
            description = "Atualiza somente campos não nulos: name não vazio é aparado e substituído; name vazio é preservado. description não nula é aparada e substituída, inclusive para texto vazio.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ferramenta atualizada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ToolResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Payload inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão TOOLS_TOOL_UPDATE.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Ferramenta não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<ToolResponseDTO> updateTool(
            @Parameter(description = "Identificador da ferramenta.", example = "42", required = true)
            @PathVariable Long id,
            @Valid @RequestBody ToolUpdateRequestDTO request) {
        var tool = adminToolService.updateTool(id, request.name(), request.description());
        return ResponseEntity.ok(new ToolResponseDTO(tool));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('TOOLS_TOOL_ACTIVATE')")
    @RequiredPermission("TOOLS_TOOL_ACTIVATE")
    @Operation(
            summary = "Ativar ferramenta administrativamente",
            description = "Muda INACTIVE para ACTIVE. Quando já está ACTIVE, a operação é idempotente, retorna o mesmo recurso e não persiste mutação.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ferramenta retornada com status ACTIVE.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ToolResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão TOOLS_TOOL_ACTIVATE.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Ferramenta não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<ToolResponseDTO> activateTool(
            @Parameter(description = "Identificador da ferramenta.", example = "42", required = true)
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(new ToolResponseDTO(adminToolService.activateTool(id)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('TOOLS_TOOL_DEACTIVATE')")
    @RequiredPermission("TOOLS_TOOL_DEACTIVATE")
    @Operation(
            summary = "Desativar ferramenta administrativamente",
            description = "Muda ACTIVE para INACTIVE. Quando já está INACTIVE, a operação é idempotente, retorna o mesmo recurso e não persiste mutação.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ferramenta retornada com status INACTIVE.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ToolResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão TOOLS_TOOL_DEACTIVATE.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Ferramenta não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<ToolResponseDTO> deactivateTool(
            @Parameter(description = "Identificador da ferramenta.", example = "42", required = true)
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(new ToolResponseDTO(adminToolService.deactivateTool(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TOOLS_TOOL_DELETE')")
    @RequiredPermission("TOOLS_TOOL_DELETE")
    @Operation(
            summary = "Excluir ferramenta administrativamente",
            description = "Com lock pessimista, grava snapshot histórico com o userId administrativo e remove fisicamente uma ferramenta ACTIVE ou INACTIVE.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Ferramenta removida e histórico arquivado."),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Usuário sem a permissão TOOLS_TOOL_DELETE.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Ferramenta não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Ferramenta removida concorrentemente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<Void> deleteTool(
            @Parameter(description = "Identificador da ferramenta.", example = "42", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        adminToolService.deleteTool(id, userPrincipal.getUserId());
        return ResponseEntity.noContent().build();
    }
}
