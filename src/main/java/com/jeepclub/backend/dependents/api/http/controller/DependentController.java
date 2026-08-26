package com.jeepclub.backend.dependents.api.http.controller;

import com.jeepclub.backend.dependents.api.http.dto.dependent.CreateDependentRequestDTO;
import com.jeepclub.backend.dependents.api.http.dto.dependent.DependentResponseDTO;
import com.jeepclub.backend.dependents.api.http.dto.dependent.UpdateDependentRequestDTO;
import com.jeepclub.backend.dependents.core.application.result.DependentResult;
import com.jeepclub.backend.dependents.core.application.service.dependent.DependentService;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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

    @PostMapping
    @Operation(
            summary = "Adicionar dependente",
            description = "Cadastra um novo dependente associado ao usuário autenticado."
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
            description = "Retorna os dependentes ativos associados ao usuário autenticado."
    )
    public ResponseEntity<List<DependentResponseDTO>> getMyDependents(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<DependentResponseDTO> response = dependentService
                .findAllByUserId(principal.getUserId())
                .stream()
                .map(DependentResponseDTO::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Consultar dependente",
            description = "Retorna um dependente ativo pertencente ao usuário autenticado."
    )
    public ResponseEntity<DependentResponseDTO> getMyDependentById(
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

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar dependente",
            description = "Atualiza um dependente ativo pertencente ao usuário autenticado."
    )
    public ResponseEntity<DependentResponseDTO> update(
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
            description = "Remove o dependente do cadastro operacional, preservando seu registro histórico para auditoria."
    )
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        dependentService.delete(
                id,
                principal.getUserId()
        );

        return ResponseEntity.noContent().build();
    }
}
