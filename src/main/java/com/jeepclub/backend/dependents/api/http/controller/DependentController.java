package com.jeepclub.backend.dependents.api.http.controller;

import com.jeepclub.backend.dependents.api.http.dto.dependent.CreateDependentRequestDTO;
import com.jeepclub.backend.dependents.api.http.dto.dependent.DependentResponseDTO;
import com.jeepclub.backend.dependents.api.http.dto.dependent.UpdateDependentRequestDTO;
import com.jeepclub.backend.dependents.core.application.result.DependentResult;
import com.jeepclub.backend.dependents.core.application.service.dependent.DependentService;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dependents")
@RequiredArgsConstructor
@Tag(
        name = "Dependents - Dependent",
        description = "Gerenciamento dos dependentes do usuário autenticado."
)
public class DependentController {

    private final DependentService dependentService;

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Adicionar dependente",
            description = "Cadastra um novo dependente associado ao usuário autenticado.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Dependente criado.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = DependentResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Dados do dependente inválidos.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Usuário titular não encontrado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Usuário titular inativo ou CPF já utilizado por usuário ou dependente operacional.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<DependentResponseDTO> create(
            @RequestBody @Valid CreateDependentRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        DependentResult result = dependentService.create(
                request.name(),
                request.cpf(),
                request.birthDate(),
                request.relationshipType(),
                request.phoneNumber(),
                principal.getUserId()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(DependentResponseDTO.from(result));
    }

    @GetMapping
    @Operation(
            summary = "Listar meus dependentes",
            description = "Retorna os dependentes ativos associados ao usuário autenticado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dependentes ativos retornados.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(schema = @Schema(implementation = DependentResponseDTO.class)))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<List<DependentResponseDTO>> getMyDependents(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue="ACTIVE") com.jeepclub.backend.dependents.core.domain.enums.DependentStatus status
    ) {
        var values = status == com.jeepclub.backend.dependents.core.domain.enums.DependentStatus.ACTIVE
                ? dependentService.findAllByUserId(principal.getUserId())
                : dependentService.findAllByUserId(principal.getUserId(),status);
        List<DependentResponseDTO> response = values.stream()
                .map(DependentResponseDTO::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Consultar dependente",
            description = "Retorna o cadastro do próprio dependente, incluindo INACTIVE e ARCHIVED.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dependente ativo retornado.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = DependentResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Formato do identificador inválido.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Dependente não pertence ao usuário autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Dependente não encontrado ou não está ativo.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<DependentResponseDTO> getMyDependentById(
            @Parameter(description = "Identificador do dependente.", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        DependentResult result = dependentService.findById(
                id,
                principal.getUserId()
        );

        return ResponseEntity.ok(
                DependentResponseDTO.from(result)
        );
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Atualizar dependente",
            description = "Atualiza um dependente ativo pertencente ao usuário autenticado.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Dependente atualizado.",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = DependentResponseDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Identificador ou dados do dependente inválidos.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Dependente não pertence ao usuário autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Dependente não encontrado ou não está ativo.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "CPF já utilizado por usuário ou dependente operacional.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<DependentResponseDTO> update(
            @Parameter(description = "Identificador do dependente.", required = true)
            @PathVariable Long id,
            @RequestBody @Valid UpdateDependentRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        DependentResult result = dependentService.update(
                id,
                request.name(),
                request.cpf(),
                request.birthDate(),
                request.relationshipType(),
                request.phoneNumber(),
                principal.getUserId()
        );

        return ResponseEntity.ok(
                DependentResponseDTO.from(result)
        );
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Remover dependente",
            description = "Remove o dependente do cadastro operacional, preservando seu registro histórico para auditoria.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Dependente removido do cadastro operacional."),
                    @ApiResponse(responseCode = "400", description = "Formato do identificador inválido.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Dependente não pertence ao usuário autenticado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Dependente não encontrado.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Dependente já foi removido por uma operação concorrente.",
                            content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE,
                                    schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador do dependente.", required = true)
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        dependentService.delete(
                id,
                principal.getUserId()
        );

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/disable")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    @Operation(summary = "disable meu dependente", description="HTTP 409 DEPENDENT_LIFECYCLE_CONFLICT para participacao operacional em Event nao encerrado; sem cancelamento ou desassociacao. Historico e permitido.")
    @ApiResponse(responseCode="409", description="Transição incompatível.", content=@Content(mediaType=MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema=@Schema(implementation=ApiErrorResponse.class)))
    public ResponseEntity<DependentResponseDTO> disable(@PathVariable @jakarta.validation.constraints.Positive Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(DependentResponseDTO.from(dependentService.disable(id,principal.getUserId())));
    }

    @PatchMapping("/{id}/enable")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    @Operation(summary = "enable meu dependente", description = "Ownership obrigatório; unarchive retorna a INACTIVE e não ativa automaticamente.")
    @ApiResponse(responseCode="409", description="Transição incompatível.", content=@Content(mediaType=MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema=@Schema(implementation=ApiErrorResponse.class)))
    public ResponseEntity<DependentResponseDTO> enable(@PathVariable @jakarta.validation.constraints.Positive Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(DependentResponseDTO.from(dependentService.enable(id,principal.getUserId())));
    }

    @PatchMapping("/{id}/archive")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    @Operation(summary = "archive meu dependente", description="HTTP 409 DEPENDENT_LIFECYCLE_CONFLICT para participacao operacional em Event nao encerrado; sem cancelamento ou desassociacao. Historico e permitido.")
    @ApiResponse(responseCode="409", description="Transição incompatível.", content=@Content(mediaType=MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema=@Schema(implementation=ApiErrorResponse.class)))
    public ResponseEntity<DependentResponseDTO> archive(@PathVariable @jakarta.validation.constraints.Positive Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(DependentResponseDTO.from(dependentService.archive(id,principal.getUserId(),principal.getUserId())));
    }

    @PatchMapping("/{id}/unarchive")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    @Operation(summary = "unarchive meu dependente", description = "Ownership obrigatório; unarchive retorna a INACTIVE e não ativa automaticamente.")
    @ApiResponse(responseCode="409", description="Transição incompatível.", content=@Content(mediaType=MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema=@Schema(implementation=ApiErrorResponse.class)))
    public ResponseEntity<DependentResponseDTO> unarchive(@PathVariable @jakarta.validation.constraints.Positive Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(DependentResponseDTO.from(dependentService.unarchive(id,principal.getUserId(),principal.getUserId())));
    }
}
