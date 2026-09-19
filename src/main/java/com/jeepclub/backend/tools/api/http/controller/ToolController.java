package com.jeepclub.backend.tools.api.http.controller;

import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.pagination.PageResponse;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import com.jeepclub.backend.tools.api.http.dto.ToolCreateRequestDTO;
import com.jeepclub.backend.tools.api.http.dto.ToolResponseDTO;
import com.jeepclub.backend.tools.api.http.dto.ToolSummaryResponseDTO;
import com.jeepclub.backend.tools.api.http.dto.ToolSummaryPageResponseSchema;
import com.jeepclub.backend.tools.api.http.dto.ToolUpdateRequestDTO;
import com.jeepclub.backend.tools.core.application.service.tool.ToolService;

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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tools")
@RequiredArgsConstructor
@Tag(
        name = "Tools - Member",
        description = "Operações do membro autenticado sobre suas próprias ferramentas."
)
public class ToolController {

    private final ToolService toolService;

    @GetMapping
    @Operation(
            summary = "Listar ferramentas do membro autenticado",
            description = "Retorna uma página das ferramentas do membro sem filtro implícito de status; itens ACTIVE e INACTIVE podem aparecer. A paginação usa os defaults globais: página zero-based e size 20, limitado a 50.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Página de ferramentas retornada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ToolSummaryPageResponseSchema.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<PageResponse<ToolSummaryResponseDTO>> getAvailableTools(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @ParameterObject Pageable pageable) {
        PageResponse<ToolSummaryResponseDTO> tools = PageResponse.from(
                toolService.listUserTools(userPrincipal.getUserId(), pageable)
                        .map(ToolSummaryResponseDTO::new));
        return ResponseEntity.ok(tools);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Consultar ferramenta do membro autenticado",
            description = "Retorna uma ferramenta ACTIVE ou INACTIVE quando pertence ao membro autenticado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ferramenta retornada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ToolResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Ferramenta pertence a outro usuário.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Ferramenta não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<ToolResponseDTO> getToolById(
            @Parameter(description = "Identificador da ferramenta.", example = "42", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ToolResponseDTO tool = new ToolResponseDTO(toolService.getToolDetails(id, userPrincipal.getUserId()));
        return ResponseEntity.ok(tool);
    }

    @PostMapping
    @Operation(
            summary = "Criar ferramenta do membro autenticado",
            description = "Cria uma ferramenta ACTIVE para o userId do principal autenticado.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Ferramenta criada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ToolResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Payload inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<ToolResponseDTO> createTool(
            @Valid @RequestBody ToolCreateRequestDTO request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ToolResponseDTO createdTool = new ToolResponseDTO(toolService.createTool(
                request.name(),
                request.description(),
                userPrincipal.getUserId()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(createdTool);
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar parcialmente uma ferramenta do membro",
            description = "Atualiza somente campos não nulos: name não vazio é aparado e substituído; name vazio é preservado. description não nula é aparada e substituída, inclusive para texto vazio.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ferramenta atualizada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ToolResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Payload inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Ferramenta pertence a outro usuário.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Ferramenta não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<ToolResponseDTO> updateTool(
            @Parameter(description = "Identificador da ferramenta.", example = "42", required = true)
            @PathVariable Long id,
            @Valid @RequestBody ToolUpdateRequestDTO request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ToolResponseDTO updatedTool = new ToolResponseDTO(toolService.updateTool(
                id,
                request.name(),
                request.description(),
                userPrincipal.getUserId()
        ));
        return ResponseEntity.ok(updatedTool);
    }

    @PatchMapping("/{id}/activate")
    @Operation(
            summary = "Ativar ferramenta do membro",
            description = "Muda INACTIVE para ACTIVE. Quando já está ACTIVE, a operação é idempotente, retorna o mesmo recurso e não persiste mutação.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ferramenta retornada com status ACTIVE.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ToolResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Ferramenta pertence a outro usuário.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Ferramenta não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<ToolResponseDTO> activateTool(
            @Parameter(description = "Identificador da ferramenta.", example = "42", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ToolResponseDTO updatedTool = new ToolResponseDTO(toolService.activateTool(id, userPrincipal.getUserId()));
        return ResponseEntity.ok(updatedTool);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(
            summary = "Desativar ferramenta do membro",
            description = "Muda ACTIVE para INACTIVE. Quando já está INACTIVE, a operação é idempotente, retorna o mesmo recurso e não persiste mutação.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Ferramenta retornada com status INACTIVE.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ToolResponseDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Ferramenta pertence a outro usuário.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Ferramenta não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<ToolResponseDTO> deactivateTool(
            @Parameter(description = "Identificador da ferramenta.", example = "42", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        ToolResponseDTO updatedTool = new ToolResponseDTO(toolService.deactivateTool(id, userPrincipal.getUserId()));
        return ResponseEntity.ok(updatedTool);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Excluir ferramenta do membro",
            description = "Com lock pessimista, grava snapshot histórico e remove fisicamente a ferramenta ACTIVE ou INACTIVE do membro.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Ferramenta removida e histórico arquivado."),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Ferramenta pertence a outro usuário.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Ferramenta não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Ferramenta removida concorrentemente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<Void> deleteTool(
            @Parameter(description = "Identificador da ferramenta.", example = "42", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        toolService.deleteTool(id, userPrincipal.getUserId());
        return ResponseEntity.noContent().build();
    }
}
