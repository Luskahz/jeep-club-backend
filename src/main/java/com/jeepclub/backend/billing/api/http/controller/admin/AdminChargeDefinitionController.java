package com.jeepclub.backend.billing.api.http.controller.admin;

import com.jeepclub.backend.billing.api.http.dto.definition.ChargeDefinitionRequest;
import com.jeepclub.backend.billing.api.http.dto.BillingPageSchemas;
import com.jeepclub.backend.billing.api.http.dto.definition.ChargeDefinitionResponse;
import com.jeepclub.backend.billing.api.http.dto.definition.ChargeDefinitionSummaryResponse;
import com.jeepclub.backend.billing.api.http.dto.definition.ChargeDefinitionUpdateRequest;
import com.jeepclub.backend.billing.core.application.result.ChargeDefinitionResult;
import com.jeepclub.backend.billing.core.application.service.chargedefinition.AdminChargeDefinitionService;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
import com.jeepclub.backend.platform.web.pagination.PageResponse;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@RequestMapping("/billing/charge-definitions")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Billing - Charge Definitions",
        description = "Endpoints administrativos para gerenciamento de definições de cobrança."
)
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Permissão administrativa ausente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class AdminChargeDefinitionController {

    private final AdminChargeDefinitionService adminChargeDefinitionService;

    @PostMapping
    @PreAuthorize("hasAuthority('BILLING_CHARGE_DEFINITION_CREATE')")
    @RequiredPermission("BILLING_CHARGE_DEFINITION_CREATE")
    @ApiResponse(responseCode = "400", description = "Payload inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Já existe definição com o mesmo nome.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Criar definição de cobrança",
            description = "Cria um modelo de cobrança que poderá gerar débitos para membros em ciclos ou contextos específicos."
    )
    public ResponseEntity<ChargeDefinitionResponse> create(
            @Valid @RequestBody ChargeDefinitionRequest request
    ) {
        ChargeDefinitionResult result = adminChargeDefinitionService.create(
                request.name(),
                request.description(),
                request.defaultAmount(),
                request.recurrenceType(),
                request.required(),
                request.paymentAcceptancePolicy(),
                request.latePaymentGraceDays()
        );

        return ResponseEntity
                .created(URI.create("/billing/charge-definitions/" + result.id()))
                .body(ChargeDefinitionResponse.from(result));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_DEFINITION_UPDATE')")
    @RequiredPermission("BILLING_CHARGE_DEFINITION_UPDATE")
    @ApiResponse(responseCode = "400", description = "Payload inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Definição não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Definição arquivada ou nome duplicado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Atualizar definição de cobrança",
            description = "Atualiza os dados principais de uma definição de cobrança. A alteração afeta apenas usos futuros da definição."
    )
    public ResponseEntity<ChargeDefinitionResponse> update(
            @PathVariable @Positive(message = "ID deve ser maior que zero.") Long id,
            @Valid @RequestBody ChargeDefinitionUpdateRequest request
    ) {
        ChargeDefinitionResult result = adminChargeDefinitionService.update(
                id,
                request.name(),
                request.description(),
                request.defaultAmount(),
                request.recurrenceType(),
                request.required(),
                request.paymentAcceptancePolicy(),
                request.latePaymentGraceDays()
        );

        return ResponseEntity.ok(ChargeDefinitionResponse.from(result));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('BILLING_CHARGE_DEFINITION_READ')")
    @RequiredPermission("BILLING_CHARGE_DEFINITION_READ")
    @Operation(
            summary = "Listar definições de cobrança",
            description = "Lista os modelos de cobrança de forma paginada. Usa page zero-based, size 20 por padrão e limite global de 50.",
            responses = @ApiResponse(responseCode = "200", description = "Página de definições retornada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BillingPageSchemas.ChargeDefinitions.class)))
    )
    public ResponseEntity<PageResponse<ChargeDefinitionSummaryResponse>> findAll(
            @ParameterObject Pageable pageable
    ) {
        Page<ChargeDefinitionResult> results = adminChargeDefinitionService.findAll(pageable);

        return ResponseEntity.ok(PageResponse.from(results.map(ChargeDefinitionSummaryResponse::from)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_DEFINITION_READ')")
    @RequiredPermission("BILLING_CHARGE_DEFINITION_READ")
    @ApiResponse(responseCode = "404", description = "Definição não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Buscar definição de cobrança por ID",
            description = "Consulta os dados de uma definição de cobrança específica."
    )
    public ResponseEntity<ChargeDefinitionResponse> findById(
            @PathVariable @Positive(message = "ID deve ser maior que zero.") Long id
    ) {
        ChargeDefinitionResult result = adminChargeDefinitionService.findById(id);

        return ResponseEntity.ok(ChargeDefinitionResponse.from(result));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_DEFINITION_UPDATE')")
    @RequiredPermission("BILLING_CHARGE_DEFINITION_UPDATE")
    @ApiResponse(responseCode = "404", description = "Definição não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Definição arquivada não pode ser ativada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Ativar definição de cobrança",
            description = "Ativa uma definição de cobrança para permitir novas utilizações."
    )
    public ResponseEntity<ChargeDefinitionResponse> activate(
            @PathVariable @Positive(message = "ID deve ser maior que zero.") Long id
    ) {
        ChargeDefinitionResult result = adminChargeDefinitionService.activate(id);

        return ResponseEntity.ok(ChargeDefinitionResponse.from(result));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_DEFINITION_UPDATE')")
    @RequiredPermission("BILLING_CHARGE_DEFINITION_UPDATE")
    @ApiResponse(responseCode = "404", description = "Definição não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Definição arquivada não pode ser desativada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Desativar definição de cobrança",
            description = "Desativa uma definição de cobrança sem removê-la do histórico."
    )
    public ResponseEntity<ChargeDefinitionResponse> deactivate(
            @PathVariable @Positive(message = "ID deve ser maior que zero.") Long id
    ) {
        ChargeDefinitionResult result = adminChargeDefinitionService.deactivate(id);

        return ResponseEntity.ok(ChargeDefinitionResponse.from(result));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_DEFINITION_UPDATE')")
    @RequiredPermission("BILLING_CHARGE_DEFINITION_UPDATE")
    @ApiResponse(responseCode = "404", description = "Definição não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Definição já arquivada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Arquivar definição de cobrança",
            description = "Arquiva uma definição de cobrança, impedindo sua reativação no fluxo normal."
    )
    public ResponseEntity<ChargeDefinitionResponse> archive(
            @PathVariable @Positive(message = "ID deve ser maior que zero.") Long id
    ) {
        ChargeDefinitionResult result = adminChargeDefinitionService.archive(id);

        return ResponseEntity.ok(ChargeDefinitionResponse.from(result));
    }
}
