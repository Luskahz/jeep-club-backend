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
@RequestMapping("/socios/{socioId}/dependents")
@RequiredArgsConstructor
@Tag(
        name = "Dependents - Admin",
        description = "Consulta administrativa dos dependentes vinculados aos sócios."
)
public class AdminDependentController {

    private final AdminDependentService adminDependentService;

    @GetMapping
    @PreAuthorize("hasAuthority('DEPENDENTS_DEPENDENT_READ')")
    @RequiredPermission("DEPENDENTS_DEPENDENT_READ")
    @Operation(
            summary = "Listar dependentes de um sócio",
            description = "Lista os dependentes vinculados ao sócio informado, incluindo registros excluídos logicamente."
    )
    public ResponseEntity<List<DependentResponseDTO>> getDependentsBySocioId(
            @Parameter(
                    description = "Identificador do sócio titular.",
                    required = true
            )
            @PathVariable Long socioId
    ) {
        List<DependentResponseDTO> response = adminDependentService
                .findAllBySocioId(socioId)
                .stream()
                .map(DependentResponseDTO::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DEPENDENTS_DEPENDENT_READ')")
    @RequiredPermission("DEPENDENTS_DEPENDENT_READ")
    @Operation(
            summary = "Consultar dependente de um sócio",
            description = "Consulta um dependente específico vinculado ao sócio informado, inclusive se estiver excluído logicamente."
    )
    public ResponseEntity<DependentResponseDTO> getDependentBySocioAndId(
            @Parameter(
                    description = "Identificador do sócio titular.",
                    required = true
            )
            @PathVariable Long socioId,

            @Parameter(
                    description = "Identificador do dependente.",
                    required = true
            )
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                DependentResponseDTO.from(
                        adminDependentService.findBySocioIdAndId(
                                socioId,
                                id
                        )
                )
        );
    }
}