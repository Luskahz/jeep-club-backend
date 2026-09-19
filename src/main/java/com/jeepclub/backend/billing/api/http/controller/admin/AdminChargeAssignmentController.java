package com.jeepclub.backend.billing.api.http.controller.admin;

import com.jeepclub.backend.billing.api.http.dto.assignment.ChargeAssignmentResponse;
import com.jeepclub.backend.billing.api.http.dto.BillingPageSchemas;
import com.jeepclub.backend.billing.core.application.result.ChargeAssignmentResult;
import com.jeepclub.backend.billing.core.application.service.chargeassignment.AdminChargeAssignmentService;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.web.pagination.PageResponse;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
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
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Permissão administrativa ausente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class AdminChargeAssignmentController {

    private final AdminChargeAssignmentService adminChargeAssignmentService;

    @PostMapping("/billing/charge-definitions/{chargeDefinitionId}/assignments/all-members")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_ASSIGNMENT_CREATE')")
    @RequiredPermission("BILLING_CHARGE_ASSIGNMENT_CREATE")
    @ApiResponse(responseCode = "404", description = "Definição ativa não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Atribuição já existe ou definição não aceita alteração.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Atribuir cobrança a todos os membros",
            description = "Cria uma regra para aplicar a definição de cobrança a todos os membros elegíveis."
    )
    public ResponseEntity<ChargeAssignmentResponse> assignToAllMembers(
            @PathVariable @Positive(message = "ID da definição de cobrança deve ser maior que zero.") Long chargeDefinitionId
    ) {
        ChargeAssignmentResult result = adminChargeAssignmentService.assignToAllMembers(chargeDefinitionId);

        return ResponseEntity
                .created(URI.create("/billing/charge-definitions/" + chargeDefinitionId + "/assignments/" + result.id()))
                .body(ChargeAssignmentResponse.from(result));
    }

    @PostMapping("/billing/charge-definitions/{chargeDefinitionId}/assignments/users/{userId}")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_ASSIGNMENT_CREATE')")
    @RequiredPermission("BILLING_CHARGE_ASSIGNMENT_CREATE")
    @ApiResponse(responseCode = "404", description = "Definição ou membro ativo não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Atribuição já existe ou definição não aceita alteração.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Atribuir cobrança a um usuário",
            description = "Cria uma regra para aplicar a definição de cobrança a um usuário específico."
    )
    public ResponseEntity<ChargeAssignmentResponse> assignToUser(
            @PathVariable @Positive(message = "ID da definição de cobrança deve ser maior que zero.") Long chargeDefinitionId,
            @PathVariable @Positive(message = "ID do usuário deve ser maior que zero.") Long userId
    ) {
        ChargeAssignmentResult result = adminChargeAssignmentService.assignToUser(
                chargeDefinitionId,
                userId
        );

        return ResponseEntity
                .created(URI.create("/billing/charge-definitions/" + chargeDefinitionId + "/assignments/" + result.id()))
                .body(ChargeAssignmentResponse.from(result));
    }

    @PostMapping("/billing/charge-definitions/{chargeDefinitionId}/assignments/roles/{roleId}")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_ASSIGNMENT_CREATE')")
    @RequiredPermission("BILLING_CHARGE_ASSIGNMENT_CREATE")
    @ApiResponse(responseCode = "404", description = "Definição ou role ativa não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Atribuição já existe ou definição não aceita alteração.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Atribuir cobrança a uma role",
            description = "Cria uma regra para aplicar a definição de cobrança a usuários associados a uma role específica."
    )
    public ResponseEntity<ChargeAssignmentResponse> assignToRole(
            @PathVariable @Positive(message = "ID da definição de cobrança deve ser maior que zero.") Long chargeDefinitionId,
            @PathVariable @Positive(message = "ID da role deve ser maior que zero.") Long roleId
    ) {
        ChargeAssignmentResult result = adminChargeAssignmentService.assignToRole(
                chargeDefinitionId,
                roleId
        );

        return ResponseEntity
                .created(URI.create("/billing/charge-definitions/" + chargeDefinitionId + "/assignments/" + result.id()))
                .body(ChargeAssignmentResponse.from(result));
    }

    @PostMapping("/billing/charge-definitions/{chargeDefinitionId}/assignments/events/{eventId}/participants")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_ASSIGNMENT_CREATE')")
    @RequiredPermission("BILLING_CHARGE_ASSIGNMENT_CREATE")
    @ApiResponse(responseCode = "404", description = "Definição ou evento não encontrado; integração de eventos está indisponível no adapter atual.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Atribuição já existe ou definição não aceita alteração.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Atribuir cobrança aos participantes de um evento",
            description = "Cria uma regra para aplicar a definição de cobrança aos participantes confirmados de um evento."
    )
    public ResponseEntity<ChargeAssignmentResponse> assignToEventParticipants(
            @PathVariable @Positive(message = "ID da definição de cobrança deve ser maior que zero.") Long chargeDefinitionId,
            @PathVariable @Positive(message = "ID do evento deve ser maior que zero.") Long eventId
    ) {
        ChargeAssignmentResult result = adminChargeAssignmentService.assignToEventParticipants(
                chargeDefinitionId,
                eventId
        );

        return ResponseEntity
                .created(URI.create("/billing/charge-definitions/" + chargeDefinitionId + "/assignments/" + result.id()))
                .body(ChargeAssignmentResponse.from(result));
    }

    @GetMapping("/billing/charge-definitions/{chargeDefinitionId}/assignments")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_ASSIGNMENT_READ')")
    @RequiredPermission("BILLING_CHARGE_ASSIGNMENT_READ")
    @ApiResponse(responseCode = "404", description = "Definição não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Listar atribuições de uma definição de cobrança",
            description = "Lista as atribuições da definição de forma paginada, usando page zero-based, size 20 por padrão e limite global de 50.",
            responses = @ApiResponse(responseCode = "200", description = "Página de atribuições retornada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BillingPageSchemas.ChargeAssignments.class)))
    )
    public ResponseEntity<PageResponse<ChargeAssignmentResponse>> findByChargeDefinitionId(
            @PathVariable @Positive(message = "ID da definição de cobrança deve ser maior que zero.") Long chargeDefinitionId,
            @ParameterObject Pageable pageable
    ) {
        Page<ChargeAssignmentResult> results = adminChargeAssignmentService.findByChargeDefinitionId(
                chargeDefinitionId,
                pageable
        );

        return ResponseEntity.ok(PageResponse.from(results.map(ChargeAssignmentResponse::from)));
    }
    @GetMapping("/billing/charge-assignments/{assignmentId}")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_ASSIGNMENT_READ')")
    @RequiredPermission("BILLING_CHARGE_ASSIGNMENT_READ")
    @ApiResponse(responseCode = "404", description = "Atribuição não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Buscar atribuição de cobrança por ID",
            description = "Consulta os dados de uma regra de atribuição de cobrança específica."
    )
    public ResponseEntity<ChargeAssignmentResponse> findById(
            @PathVariable @Positive(message = "ID da atribuição deve ser maior que zero.") Long assignmentId
    ) {
        ChargeAssignmentResult result = adminChargeAssignmentService.findById(assignmentId);

        return ResponseEntity.ok(ChargeAssignmentResponse.from(result));
    }

    @PatchMapping("/billing/charge-assignments/{assignmentId}/activate")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_ASSIGNMENT_UPDATE')")
    @RequiredPermission("BILLING_CHARGE_ASSIGNMENT_UPDATE")
    @ApiResponse(responseCode = "404", description = "Atribuição ou definição não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Atribuição já ativa ou definição não está ativa.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Ativar atribuição de cobrança",
            description = "Ativa uma regra de atribuição de cobrança."
    )
    public ResponseEntity<ChargeAssignmentResponse> activate(
            @PathVariable @Positive(message = "ID da atribuição deve ser maior que zero.") Long assignmentId
    ) {
        ChargeAssignmentResult result = adminChargeAssignmentService.activate(assignmentId);

        return ResponseEntity.ok(ChargeAssignmentResponse.from(result));
    }

    @PatchMapping("/billing/charge-assignments/{assignmentId}/deactivate")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_ASSIGNMENT_UPDATE')")
    @RequiredPermission("BILLING_CHARGE_ASSIGNMENT_UPDATE")
    @ApiResponse(responseCode = "404", description = "Atribuição ou definição não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Atribuição já inativa ou definição arquivada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Desativar atribuição de cobrança",
            description = "Desativa uma regra de atribuição de cobrança."
    )
    public ResponseEntity<ChargeAssignmentResponse> deactivate(
            @PathVariable @Positive(message = "ID da atribuição deve ser maior que zero.") Long assignmentId
    ) {
        ChargeAssignmentResult result = adminChargeAssignmentService.deactivate(assignmentId);

        return ResponseEntity.ok(ChargeAssignmentResponse.from(result));
    }
}
