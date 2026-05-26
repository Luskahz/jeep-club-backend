package com.jeepclub.backend.billing.api.controller;

import com.jeepclub.backend.billing.api.dto.ChargeAssignmentResponse;
import com.jeepclub.backend.billing.core.application.result.ChargeAssignmentResult;
import com.jeepclub.backend.billing.core.application.service.ChargeAssignmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Billing - Charge Assignments",
        description = "Endpoints administrativos para gerenciamento de regras de atribuição de cobranças."
)
public class ChargeAssignmentController {

    private final ChargeAssignmentService chargeAssignmentService;

    @PostMapping("/billing/charge-definitions/{chargeDefinitionId}/assignments/all-members")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_ASSIGNMENT_CREATE')")
    @Operation(
            summary = "Atribuir cobrança a todos os membros",
            description = "Cria uma regra para aplicar a definição de cobrança a todos os membros elegíveis."
    )
    public ResponseEntity<ChargeAssignmentResponse> assignToAllMembers(
            @PathVariable @Positive(message = "ID da definição de cobrança deve ser maior que zero.") Long chargeDefinitionId
    ) {
        ChargeAssignmentResult result = chargeAssignmentService.assignToAllMembers(chargeDefinitionId);

        return ResponseEntity
                .created(URI.create("/billing/charge-definitions/" + chargeDefinitionId + "/assignments/" + result.id()))
                .body(ChargeAssignmentResponse.from(result));
    }


    @PostMapping("/billing/charge-definitions/{chargeDefinitionId}/assignments/users/{userId}")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_ASSIGNMENT_CREATE')")
    @Operation(
            summary = "Atribuir cobrança a um usuário",
            description = "Cria uma regra para aplicar a definição de cobrança a um usuário específico."
    )
    public ResponseEntity<ChargeAssignmentResponse> assignToUser(
            @PathVariable @Positive(message = "ID da definição de cobrança deve ser maior que zero.") Long chargeDefinitionId,
            @PathVariable @Positive(message = "ID do usuário deve ser maior que zero.") Long userId
    ) {
        ChargeAssignmentResult result = chargeAssignmentService.assignToUser(
                chargeDefinitionId,
                userId
        );

        return ResponseEntity
                .created(URI.create("/billing/charge-definitions/" + chargeDefinitionId + "/assignments/" + result.id()))
                .body(ChargeAssignmentResponse.from(result));
    }

    @PostMapping("/billing/charge-definitions/{chargeDefinitionId}/assignments/roles/{roleId}")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_ASSIGNMENT_CREATE')")
    @Operation(
            summary = "Atribuir cobrança a uma role",
            description = "Cria uma regra para aplicar a definição de cobrança a usuários associados a uma role específica."
    )
    public ResponseEntity<ChargeAssignmentResponse> assignToRole(
            @PathVariable @Positive(message = "ID da definição de cobrança deve ser maior que zero.") Long chargeDefinitionId,
            @PathVariable @Positive(message = "ID da role deve ser maior que zero.") Long roleId
    ) {
        ChargeAssignmentResult result = chargeAssignmentService.assignToRole(
                chargeDefinitionId,
                roleId
        );

        return ResponseEntity
                .created(URI.create("/billing/charge-definitions/" + chargeDefinitionId + "/assignments/" + result.id()))
                .body(ChargeAssignmentResponse.from(result));
    }

    @GetMapping("/billing/charge-definitions/{chargeDefinitionId}/assignments")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_ASSIGNMENT_READ')")
    @Operation(
            summary = "Listar atribuições de uma definição de cobrança",
            description = "Lista as regras de atribuição vinculadas a uma definição de cobrança de forma paginada."
    )
    public ResponseEntity<Page<ChargeAssignmentResponse>> findByChargeDefinitionId(
            @PathVariable @Positive(message = "ID da definição de cobrança deve ser maior que zero.") Long chargeDefinitionId,
            @ParameterObject Pageable pageable
    ) {
        Page<ChargeAssignmentResult> results = chargeAssignmentService.findByChargeDefinitionId(
                chargeDefinitionId,
                pageable
        );

        return ResponseEntity.ok(results.map(ChargeAssignmentResponse::from));
    }

    @PatchMapping("/billing/charge-assignments/{assignmentId}/activate")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_ASSIGNMENT_UPDATE')")
    @Operation(
            summary = "Ativar atribuição de cobrança",
            description = "Ativa uma regra de atribuição de cobrança."
    )
    public ResponseEntity<ChargeAssignmentResponse> activate(
            @PathVariable @Positive(message = "ID da atribuição deve ser maior que zero.") Long assignmentId
    ) {
        ChargeAssignmentResult result = chargeAssignmentService.activate(assignmentId);

        return ResponseEntity.ok(ChargeAssignmentResponse.from(result));
    }

    @PatchMapping("/billing/charge-assignments/{assignmentId}/deactivate")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_ASSIGNMENT_UPDATE')")
    @Operation(
            summary = "Desativar atribuição de cobrança",
            description = "Desativa uma regra de atribuição de cobrança."
    )
    public ResponseEntity<ChargeAssignmentResponse> deactivate(
            @PathVariable @Positive(message = "ID da atribuição deve ser maior que zero.") Long assignmentId
    ) {
        ChargeAssignmentResult result = chargeAssignmentService.deactivate(assignmentId);

        return ResponseEntity.ok(ChargeAssignmentResponse.from(result));
    }
}