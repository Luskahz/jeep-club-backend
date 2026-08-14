package com.jeepclub.backend.dependents.api.http.controller;

import com.jeepclub.backend.dependents.api.http.dto.dependent.CreateDependentRequestDTO;
import com.jeepclub.backend.dependents.api.http.dto.dependent.DependentResponseDTO;
import com.jeepclub.backend.dependents.api.http.dto.dependent.MedicalProfileDTO;
import com.jeepclub.backend.dependents.api.http.dto.dependent.UpdateDependentRequestDTO;
import com.jeepclub.backend.dependents.core.application.result.DependentResult;
import com.jeepclub.backend.dependents.core.application.service.dependent.DependentService;
import com.jeepclub.backend.dependents.core.port.DependentMedicalProfileData;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dependents")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Dependents - Dependent",
        description = "Gerenciamento de dependentes dos Sócios do Jeep Club."
)
public class DependentController {

    private final DependentService dependentService;

    @PostMapping
    @Operation(
            summary = "Adicionar dependente",
            description = "Cadastra um novo dependente associado ao Sócio autenticado. Exige a flag de consentimento LGPD aceita."
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
                request.consentAccepted(),
                toMedicalProfileData(request.medicalProfile()),
                principal.getUserId()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DependentResponseDTO.from(result));
    }

    @GetMapping
    @Operation(
            summary = "Listar meus dependentes",
            description = "Retorna a lista de dependentes cadastrados pelo Sócio autenticado."
    )
    public ResponseEntity<List<DependentResponseDTO>> getMyDependents(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<DependentResponseDTO> response = dependentService
                .findAllBySocioId(principal.getUserId())
                .stream()
                .map(DependentResponseDTO::from)
                .toList();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Consultar dependente",
            description = "Consulta os dados detalhados de um dependente específico do Sócio autenticado."
    )
    public ResponseEntity<DependentResponseDTO> getMyDependentById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(DependentResponseDTO.from(
                dependentService.findById(id, principal.getUserId())
        ));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar dependente",
            description = "Atualiza os dados de um dependente do Sócio autenticado. Exige confirmação de LGPD."
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
                request.consentAccepted(),
                toMedicalProfileData(request.medicalProfile()),
                principal.getUserId()
        );

        return ResponseEntity.ok(DependentResponseDTO.from(result));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Remover dependente",
            description = "Exclui permanentemente um dependente do Sócio autenticado."
    )
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        dependentService.delete(id, principal.getUserId());
        return ResponseEntity.noContent().build();
    }

    private DependentMedicalProfileData toMedicalProfileData(MedicalProfileDTO medicalProfile) {
        return medicalProfile == null ? null : medicalProfile.toData();
    }
}
