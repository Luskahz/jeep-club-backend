package com.jeepclub.backend.dependents.api.http.controller.admin;

import com.jeepclub.backend.dependents.api.http.dto.dependent.DependentResponseDTO;
import com.jeepclub.backend.dependents.core.application.service.dependent.AdminDependentService;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/dependents")
@RequiredArgsConstructor
@Tag(
        name = "Dependents - Admin",
        description = "Consulta administrativa dos dependentes vinculados aos usuários."
)
public class AdminDependentController {

    private final AdminDependentService adminDependentService;

    @GetMapping
    @PreAuthorize("hasAuthority('DEPENDENTS_DEPENDENT_READ')")
    @RequiredPermission("DEPENDENTS_DEPENDENT_READ")
    @Operation(
            summary = "Listar dependentes de um usuário",
            description = "Lista os dependentes operacionais vinculados ao usuário informado, incluindo ativos e desabilitados."
    )
    public ResponseEntity<List<DependentResponseDTO>> getDependentsByUserId(
            @Parameter(
                    description = "Identificador do usuário titular.",
                    required = true
            )
            @PathVariable Long userId
    ) {
        List<DependentResponseDTO> response = adminDependentService
                .findAllByUserId(userId)
                .stream()
                .map(DependentResponseDTO::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DEPENDENTS_DEPENDENT_READ')")
    @RequiredPermission("DEPENDENTS_DEPENDENT_READ")
    @Operation(
            summary = "Consultar dependente de um usuário",
            description = "Consulta um dependente operacional específico vinculado ao usuário informado, ativo ou desabilitado."
    )
    public ResponseEntity<DependentResponseDTO> getDependentByUserAndId(
            @Parameter(
                    description = "Identificador do usuário titular.",
                    required = true
            )
            @PathVariable Long userId,

            @Parameter(
                    description = "Identificador do dependente.",
                    required = true
            )
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                DependentResponseDTO.from(
                        adminDependentService.findByUserIdAndId(
                                userId,
                                id
                        )
                )
        );
    }
}
