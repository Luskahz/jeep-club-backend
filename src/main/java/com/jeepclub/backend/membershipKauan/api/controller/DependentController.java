package com.jeepclub.backend.membershipKauan.api.controller;

import com.jeepclub.backend.infra.config.openapi.security.RequiredPermission;
import com.jeepclub.backend.infra.security.principal.UserPrincipal;
import com.jeepclub.backend.membershipKauan.api.dto.dependent.CreateDependentRequestDTO;
import com.jeepclub.backend.membershipKauan.api.dto.dependent.DependentResponseDTO;
import com.jeepclub.backend.membershipKauan.api.dto.dependent.UpdateDependentRequestDTO;
import com.jeepclub.backend.membershipKauan.core.application.service.CreateDependentService;
import com.jeepclub.backend.membershipKauan.core.application.service.DeleteDependentService;
import com.jeepclub.backend.membershipKauan.core.application.service.GetDependentService;
import com.jeepclub.backend.membershipKauan.core.application.service.UpdateDependentService;
import com.jeepclub.backend.membershipKauan.core.domain.model.Dependent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Membership - Dependents",
        description = "Gerenciamento de dependentes dos Sócios do Jeep Club."
)
public class DependentController {

    private final CreateDependentService createDependentService;
    private final UpdateDependentService updateDependentService;
    private final DeleteDependentService deleteDependentService;
    private final GetDependentService getDependentService;

    // ==========================================
    // ENDPOINTS DO SÓCIO AUTENTICADO
    // ==========================================

    @PostMapping("/dependents")
    @Operation(
            summary = "Adicionar dependente",
            description = "Cadastra um novo dependente associado ao Sócio autenticado. Exige a flag de consentimento LGPD aceita."
    )
    public ResponseEntity<DependentResponseDTO> create(
            @RequestBody @Valid CreateDependentRequestDTO request,
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        
        Dependent dependent = createDependentService.create(
                request.name(),
                request.cpf(),
                request.birthDate(),
                request.relationshipType(),
                request.phoneNumber(),
                request.medicalProfile() != null ? request.medicalProfile().toDomain() : null,
                request.consentAccepted(),
                principal.getUserId()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(DependentResponseDTO.from(dependent));
    }

    @GetMapping("/dependents")
    @Operation(
            summary = "Listar meus dependentes",
            description = "Retorna a lista de dependentes cadastrados pelo Sócio autenticado."
    )
    public ResponseEntity<List<DependentResponseDTO>> getMyDependents(
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        List<Dependent> list = getDependentService.getBySocioId(principal.getUserId(), principal.getUserId(), false);
        return ResponseEntity.ok(DependentResponseDTO.from(list));
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
        return ResponseEntity.ok(DependentResponseDTO.from(dependent));
    }

    @PutMapping("/dependents/{id}")
    @Operation(
            summary = "Atualizar dependente",
            description = "Atualiza os dados de um dependente do Sócio autenticado. Exige confirmação de LGPD."
    )
    public ResponseEntity<DependentResponseDTO> update(
            @PathVariable Long id,
            @RequestBody @Valid UpdateDependentRequestDTO request,
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        
        Dependent dependent = updateDependentService.update(
                id,
                request.name(),
                request.cpf(),
                request.birthDate(),
                request.relationshipType(),
                request.phoneNumber(),
                request.medicalProfile() != null ? request.medicalProfile().toDomain() : null,
                request.consentAccepted(),
                principal.getUserId(),
                false
        );

        return ResponseEntity.ok(DependentResponseDTO.from(dependent));
    }

    @DeleteMapping("/dependents/{id}")
    @Operation(
            summary = "Remover dependente",
            description = "Exclui permanentemente um dependente do Sócio autenticado."
    )
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        deleteDependentService.delete(id, principal.getUserId(), false);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // ENDPOINTS DO DIRETOR (ADMINISTRADOR)
    // ==========================================

    @GetMapping("/socios/{socioId}/dependents")
    @PreAuthorize("hasAuthority('AUTHENTICATION_USER_READ')")
    @RequiredPermission("AUTHENTICATION_USER_READ")
    @Operation(
            summary = "Listar dependentes de um sócio (Diretor)",
            description = "Permite a um Diretor listar os dependentes de qualquer Sócio informando seu ID."
    )
    public ResponseEntity<List<DependentResponseDTO>> getDependentsBySocioId(
            @Parameter(description = "ID do Sócio titular", required = true)
            @PathVariable Long socioId
    ) {
        List<Dependent> list = getDependentService.getBySocioId(socioId, null, true);
        return ResponseEntity.ok(DependentResponseDTO.from(list));
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
        
        return ResponseEntity.ok(DependentResponseDTO.from(dependent));
    }
}
