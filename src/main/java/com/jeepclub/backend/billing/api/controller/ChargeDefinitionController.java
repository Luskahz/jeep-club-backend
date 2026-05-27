package com.jeepclub.backend.billing.api.controller;

import com.jeepclub.backend.billing.api.dto.definition.ChargeDefinitionRequest;
import com.jeepclub.backend.billing.api.dto.definition.ChargeDefinitionResponse;
import com.jeepclub.backend.billing.api.dto.definition.ChargeDefinitionSummaryResponse;
import com.jeepclub.backend.billing.core.application.result.ChargeDefinitionResult;
import com.jeepclub.backend.billing.core.application.service.ChargeDefinitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.net.URI;

@RestController
@RequestMapping("/billing/charge-definitions")
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Billing - Charge Definitions",
        description = "Endpoints administrativos para gerenciamento de definições de cobrança."
)
public class ChargeDefinitionController {

    private final ChargeDefinitionService chargeDefinitionService;

    @PostMapping
    @PreAuthorize("hasAuthority('BILLING_CHARGE_DEFINITION_CREATE')")
    @Operation(
            summary = "Criar definição de cobrança",
            description = "Cria um modelo de cobrança que poderá gerar débitos para membros em ciclos ou contextos específicos."
    )
    public ResponseEntity<ChargeDefinitionResponse> create(
            @Valid @RequestBody ChargeDefinitionRequest request
    ) {
        ChargeDefinitionResult result = chargeDefinitionService.create(
                request.name(),
                request.description(),
                request.defaultAmount(),
                request.recurrenceType(),
                request.required()
        );

        return ResponseEntity
                .created(URI.create("/billing/charge-definitions/" + result.id()))
                .body(ChargeDefinitionResponse.from(result));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_DEFINITION_UPDATE')")
    @Operation(
            summary = "Atualizar definição de cobrança",
            description = "Atualiza os dados principais de uma definição de cobrança. A alteração afeta apenas usos futuros da definição."
    )
    public ResponseEntity<ChargeDefinitionResponse> update(
            @PathVariable @Positive(message = "ID deve ser maior que zero.") Long id,
            @Valid @RequestBody ChargeDefinitionRequest request
    ) {
        ChargeDefinitionResult result = chargeDefinitionService.update(
                id,
                request.name(),
                request.description(),
                request.defaultAmount(),
                request.recurrenceType(),
                request.required()
        );

        return ResponseEntity.ok(ChargeDefinitionResponse.from(result));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('BILLING_CHARGE_DEFINITION_READ')")
    @Operation(
            summary = "Listar definições de cobrança",
            description = "Lista os modelos de cobrança cadastrados no sistema de forma paginada e resumida."
    )
    public ResponseEntity<Page<ChargeDefinitionSummaryResponse>> findAll(
            @ParameterObject Pageable pageable
    ) {
        Page<ChargeDefinitionResult> results = chargeDefinitionService.findAll(pageable);

        return ResponseEntity.ok(results.map(ChargeDefinitionSummaryResponse::from));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_DEFINITION_READ')")
    @Operation(
            summary = "Buscar definição de cobrança por ID",
            description = "Consulta os dados de uma definição de cobrança específica."
    )
    public ResponseEntity<ChargeDefinitionResponse> findById(
            @PathVariable @Positive(message = "ID deve ser maior que zero.") Long id
    ) {
        ChargeDefinitionResult result = chargeDefinitionService.findById(id);

        return ResponseEntity.ok(ChargeDefinitionResponse.from(result));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_DEFINITION_UPDATE')")
    @Operation(
            summary = "Ativar definição de cobrança",
            description = "Ativa uma definição de cobrança para permitir novas utilizações."
    )
    public ResponseEntity<ChargeDefinitionResponse> activate(
            @PathVariable @Positive(message = "ID deve ser maior que zero.") Long id
    ) {
        ChargeDefinitionResult result = chargeDefinitionService.activate(id);

        return ResponseEntity.ok(ChargeDefinitionResponse.from(result));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_DEFINITION_UPDATE')")
    @Operation(
            summary = "Desativar definição de cobrança",
            description = "Desativa uma definição de cobrança sem removê-la do histórico."
    )
    public ResponseEntity<ChargeDefinitionResponse> deactivate(
            @PathVariable @Positive(message = "ID deve ser maior que zero.") Long id
    ) {
        ChargeDefinitionResult result = chargeDefinitionService.deactivate(id);

        return ResponseEntity.ok(ChargeDefinitionResponse.from(result));
    }

    @PatchMapping("/{id}/archive")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_DEFINITION_UPDATE')")
    @Operation(
            summary = "Arquivar definição de cobrança",
            description = "Arquiva uma definição de cobrança, impedindo sua reativação no fluxo normal."
    )
    public ResponseEntity<ChargeDefinitionResponse> archive(
            @PathVariable @Positive(message = "ID deve ser maior que zero.") Long id
    ) {
        ChargeDefinitionResult result = chargeDefinitionService.archive(id);

        return ResponseEntity.ok(ChargeDefinitionResponse.from(result));
    }
}