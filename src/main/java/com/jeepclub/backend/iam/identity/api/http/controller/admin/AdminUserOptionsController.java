package com.jeepclub.backend.iam.identity.api.http.controller.admin;

import com.jeepclub.backend.iam.identity.core.application.service.user.UserOptionsService;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.web.pagination.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/identity/admin/users/options")
@Tag(name = "Identity - Users")
public class AdminUserOptionsController {
    private final UserOptionsService service;

    @Schema(description = "Opção de usuário ativo para seleção de titular. Nomes podem se repetir; use o ID como valor.")
    public record UserOptionResponse(Long id, String name) {}

    @GetMapping
    @PreAuthorize("hasAnyAuthority('IDENTITY_USER_READ', 'VEHICLES_VEHICLE_CREATE', 'TOOLS_TOOL_CREATE', 'HEALTH_MEDICAL_PROFILE_UPDATE', 'DEPENDENTS_DEPENDENT_READ')")
    @RequiredPermission({"IDENTITY_USER_READ", "VEHICLES_VEHICLE_CREATE", "TOOLS_TOOL_CREATE", "HEALTH_MEDICAL_PROFILE_UPDATE", "DEPENDENTS_DEPENDENT_READ"})
    @Operation(summary = "Listar opções de usuários para seleção administrativa",
            description = "Retorna somente id e name de usuários administrativamente ACTIVE, sem exigir associação vigente. "
                    + "Reutilizável nos cadastros de veículos, ferramentas, dados médicos e dependentes. "
                    + "name filtra por parte do nome, sem diferenciar maiúsculas e minúsculas. "
                    + "Ordenação fixa por nome e ID; page começa em zero, size vai de 1 a 50. "
                    + "Exige QUALQUER UMA das permissões documentadas. Não concede permissão para realizar os cadastros.")
    public ResponseEntity<PageResponse<UserOptionResponse>> find(
            @RequestParam(required = false) @Size(max = 150) String name,
            @RequestParam(defaultValue = "0") @Min(0) @Max(1000000) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
        var result = service.find(name, page, size).map(user -> new UserOptionResponse(user.id(), user.name()));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(PageResponse.from(result));
    }
}
