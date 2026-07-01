package com.jeepclub.backend.dependents.api.http.controller;

import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.dependents.api.http.dto.dependent.CreateDependentRequestDTO;
import com.jeepclub.backend.dependents.api.http.dto.dependent.DependentResponseDTO;
import com.jeepclub.backend.dependents.api.http.dto.dependent.UpdateDependentRequestDTO;
import com.jeepclub.backend.dependents.core.application.service.CreateDependentService;
import com.jeepclub.backend.dependents.core.application.service.DeleteDependentService;
import com.jeepclub.backend.dependents.core.application.service.GetDependentService;
import com.jeepclub.backend.dependents.core.application.service.UpdateDependentService;
import com.jeepclub.backend.dependents.core.domain.model.Dependent;
import com.jeepclub.backend.health.core.application.MedicalProfileService;
import com.jeepclub.backend.health.core.application.exceptions.MedicalProfileNotFoundException;
import com.jeepclub.backend.health.core.domain.MedicalProfile;
import com.jeepclub.backend.health.core.domain.MedicalProfileOwnerType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Dependents - Dependent",
        description = "Gerenciamento de dependentes dos Sócios do Jeep Club."
)
public class DependentController {

    private final CreateDependentService createDependentService;
    private final UpdateDependentService updateDependentService;
    private final DeleteDependentService deleteDependentService;
    private final GetDependentService getDependentService;
    private final MedicalProfileService medicalProfileService;

    // ==========================================
    // ENDPOINTS DO SÓCIO AUTENTICADO
    // ==========================================

    @PostMapping("/dependents")
    @Transactional
    @Operation(
            summary = "Adicionar dependente",
            description = "Cadastra um novo dependente associado ao Sócio autenticado. Exige a flag de consentimento LGPD aceita."
    )
    public ResponseEntity<DependentResponseDTO> create(
            @RequestBody @Valid CreateDependentRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {

        
        Dependent dependent = createDependentService.create(
                request.name(),
                request.cpf(),
                request.birthDate(),
                request.relationshipType(),
                request.phoneNumber(),
                request.consentAccepted(),
                principal.getUserId()
        );

        upsertDependentMedicalProfileIfPresent(dependent, request.medicalProfile());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DependentResponseDTO.from(
                        dependent,
                        findDependentMedicalProfile(dependent.getId())
                ));
    }

    @GetMapping("/dependents")
    @Operation(
            summary = "Listar meus dependentes",
            description = "Retorna a lista de dependentes cadastrados pelo Sócio autenticado."
    )
    public ResponseEntity<List<DependentResponseDTO>> getMyDependents(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<Dependent> list = getDependentService.getBySocioId(principal.getUserId(), principal.getUserId(), false);
        return ResponseEntity.ok(list.stream()
                .map(this::toResponseWithMedicalProfile)
                .toList());
    }

    @GetMapping("/dependents/{id}")
    @Operation(
            summary = "Consultar dependente",
            description = "Consulta os dados detalhados de um dependente específico do Sócio autenticado."
    )
    public ResponseEntity<DependentResponseDTO> getMyDependentById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        Dependent dependent = getDependentService.getById(id, principal.getUserId(), false);
        return ResponseEntity.ok(toResponseWithMedicalProfile(dependent));
    }

    @PutMapping("/dependents/{id}")
    @Transactional
    @Operation(
            summary = "Atualizar dependente",
            description = "Atualiza os dados de um dependente do Sócio autenticado. Exige confirmação de LGPD."
    )
    public ResponseEntity<DependentResponseDTO> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateDependentRequestDTO request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        
        Dependent dependent = updateDependentService.update(
                id,
                request.name(),
                request.cpf(),
                request.birthDate(),
                request.relationshipType(),
                request.phoneNumber(),
                request.consentAccepted(),
                principal.getUserId(),
                false
        );

        upsertDependentMedicalProfileIfPresent(dependent, request.medicalProfile());

        return ResponseEntity.ok(DependentResponseDTO.from(
                dependent,
                findDependentMedicalProfile(dependent.getId())
        ));
    }

    @DeleteMapping("/dependents/{id}")
    @Operation(
            summary = "Remover dependente",
            description = "Exclui permanentemente um dependente do Sócio autenticado."
    )
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        deleteDependentService.delete(id, principal.getUserId(), false);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // ENDPOINTS DO DIRETOR (ADMINISTRADOR)
    // ==========================================

    // separa esses endpoints administrativos em um controller DependentsAdminController.
    @GetMapping("/socios/{socioId}/dependents")
    @PreAuthorize("hasAuthority('DEPENDENTS_DEPENDENT_READ')")
    @RequiredPermission("DEPENDENTS_DEPENDENT_READ")
    @Operation(
            summary = "Listar dependentes de um sócio (Diretor)",
            description = "Permite a um Diretor listar os dependentes de qualquer Sócio informando seu ID."
    )
    public ResponseEntity<List<DependentResponseDTO>> getDependentsBySocioId(
            // sempre interessante que a DTO de listagem seja uma dto Sumary, ou seja, com dados resumidos, visto que mais pra frente
            // vc fornece um endpoint de consulta por id, que lá vão ter todos os atributos visiveis do dependente.
            @Parameter(description = "ID do Sócio titular", required = true)
            @PathVariable Long socioId
            // se o socio tiver 100 dependentes, o back vai servir tudo pro frontend? precisamos aplicar logica de @Pagable
    ) {
        List<Dependent> list = getDependentService.getBySocioId(socioId, null, true);
        return ResponseEntity.ok(list.stream()
                .map(this::toResponseWithMedicalProfile)
                .toList());
    }

    @GetMapping("/socios/{socioId}/dependents/{id}")
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
        Dependent dependent = getDependentService.getById(id, null, true);
        
        // Garantia de consistência: verificar se o dependente pertence mesmo ao sócio informado
        if (!dependent.getSocioId().equals(socioId)) {
            throw new IllegalArgumentException("O dependente informado não pertence ao sócio especificado.");
        }
        
        return ResponseEntity.ok(toResponseWithMedicalProfile(dependent));
    }

    private void upsertDependentMedicalProfileIfPresent(
            Dependent dependent,
            com.jeepclub.backend.dependents.api.http.dto.dependent.MedicalProfileDTO medicalProfile
    ) {
        if (medicalProfile == null || !medicalProfile.hasAnyValue()) {
            return;
        }

        medicalProfileService.upsertByOwner(
                MedicalProfileOwnerType.DEPENDENT,
                dependent.getId(),
                medicalProfile.toHealthRequest()
        );
    }

    private DependentResponseDTO toResponseWithMedicalProfile(Dependent dependent) {
        return DependentResponseDTO.from(
                dependent,
                findDependentMedicalProfile(dependent.getId())
        );
    }

    private MedicalProfile findDependentMedicalProfile(Long dependentId) {
        try {
            return medicalProfileService.getByOwner(MedicalProfileOwnerType.DEPENDENT, dependentId);
        } catch (MedicalProfileNotFoundException ex) {
            return null;
        }
    }
}
