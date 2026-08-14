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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/socios/{socioId}/dependents")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Dependents - Dependent",
        description = "Gerenciamento de dependentes dos Sócios do Jeep Club."
)
public class AdminDependentController {

    private final AdminDependentService adminDependentService;

    @GetMapping
    @PreAuthorize("hasAuthority('DEPENDENTS_DEPENDENT_READ')")
    @RequiredPermission("DEPENDENTS_DEPENDENT_READ")
    @Operation(
            summary = "Listar dependentes de um sócio (Diretor)",
            description = "Permite a um Diretor listar os dependentes de qualquer Sócio informando seu ID."
    )
    public ResponseEntity<List<DependentResponseDTO>> getDependentsBySocioId(
            @Parameter(description = "ID do Sócio titular", required = true)
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
    @PreAuthorize("hasAuthority('AUTHENTICATION_USER_READ')")
    @RequiredPermission("AUTHENTICATION_USER_READ")
    @Operation(
            summary = "Consultar dependente de um sócio (Diretor)",
            description = "Permite a um Diretor consultar os dados de um dependente específico de qualquer Sócio."
    )
    public ResponseEntity<DependentResponseDTO> getDependentBySocioAndId(
            @PathVariable Long socioId,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(DependentResponseDTO.from(
                adminDependentService.findBySocioIdAndId(socioId, id)
        ));
    }
}
