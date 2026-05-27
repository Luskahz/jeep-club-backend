package com.jeepclub.backend.billing.api.controller;

import com.jeepclub.backend.billing.api.dto.cycle.ChargeCycleResponse;
import com.jeepclub.backend.billing.api.dto.cycle.ChargeCycleSummaryResponse;
import com.jeepclub.backend.billing.api.dto.cycle.GenerateChargeCycleRequest;
import com.jeepclub.backend.billing.api.dto.cycle.GenerateChargeCycleResponse;
import com.jeepclub.backend.billing.core.application.result.cycle.ChargeCycleResult;
import com.jeepclub.backend.billing.core.application.result.cycle.GenerateChargeCycleResult;
import com.jeepclub.backend.billing.core.application.service.ChargeCycleService;
import com.jeepclub.backend.infra.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Billing - Charge Cycles",
        description = "Endpoints administrativos para geração e consulta de ciclos de cobrança."
)
public class ChargeCycleController {

    private final ChargeCycleService chargeCycleService;

    @PostMapping("/billing/charge-definitions/{chargeDefinitionId}/cycles")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_CYCLE_GENERATE')")
    @Operation(
            summary = "Gerar ciclo de cobrança",
            description = "Gera um ciclo para uma definição de cobrança e cria os débitos dos membros elegíveis."
    )
    public ResponseEntity<GenerateChargeCycleResponse> generate(
            @PathVariable @Positive(message = "ID da definição de cobrança deve ser maior que zero.") Long chargeDefinitionId,
            @Valid @RequestBody GenerateChargeCycleRequest request,
            Authentication authentication
    ) {
        Long generatedByUserId = extractUserId(authentication);

        GenerateChargeCycleResult result = chargeCycleService.generate(
                chargeDefinitionId,
                request.code(),
                request.dueDate(),
                generatedByUserId
        );

        return ResponseEntity
                .created(URI.create("/billing/charge-cycles/" + result.chargeCycle().id()))
                .body(GenerateChargeCycleResponse.from(result));
    }

    @GetMapping("/billing/charge-definitions/{chargeDefinitionId}/cycles")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_CYCLE_READ')")
    @Operation(
            summary = "Listar ciclos de uma definição de cobrança",
            description = "Lista os ciclos gerados para uma definição de cobrança de forma paginada e resumida."
    )
    public ResponseEntity<Page<ChargeCycleSummaryResponse>> findByChargeDefinitionId(
            @PathVariable @Positive(message = "ID da definição de cobrança deve ser maior que zero.") Long chargeDefinitionId,
            @ParameterObject Pageable pageable
    ) {
        Page<ChargeCycleResult> results = chargeCycleService.findByChargeDefinitionId(
                chargeDefinitionId,
                pageable
        );

        return ResponseEntity.ok(results.map(ChargeCycleSummaryResponse::from));
    }

    @GetMapping("/billing/charge-cycles/{cycleId}")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_CYCLE_READ')")
    @Operation(
            summary = "Buscar ciclo de cobrança por ID",
            description = "Consulta os dados de um ciclo de cobrança específico."
    )
    public ResponseEntity<ChargeCycleResponse> findById(
            @PathVariable @Positive(message = "ID do ciclo deve ser maior que zero.") Long cycleId
    ) {
        ChargeCycleResult result = chargeCycleService.findById(cycleId);

        return ResponseEntity.ok(ChargeCycleResponse.from(result));
    }

    // necessario definir com o matheus
    @PatchMapping("/billing/charge-cycles/{cycleId}/cancel")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_CYCLE_CANCEL')")
    @Operation(
            summary = "Cancelar ciclo de cobrança",
            description = "Cancela um ciclo de cobrança. As regras de impacto sobre débitos gerados devem ser tratadas pelo serviço."
    )
    public ResponseEntity<ChargeCycleResponse> cancel(
            @PathVariable @Positive(message = "ID do ciclo deve ser maior que zero.") Long cycleId
    ) {
        ChargeCycleResult result = chargeCycleService.cancel(cycleId);

        return ResponseEntity.ok(ChargeCycleResponse.from(result));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalArgumentException("Authenticated user principal is required.");
        }

        return principal.getUserId();
    }
}