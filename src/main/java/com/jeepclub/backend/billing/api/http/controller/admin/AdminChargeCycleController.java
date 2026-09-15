package com.jeepclub.backend.billing.api.http.controller.admin;

import com.jeepclub.backend.billing.api.http.dto.cycle.ChargeCycleResponse;
import com.jeepclub.backend.billing.api.http.dto.cycle.ChargeCycleSummaryResponse;
import com.jeepclub.backend.billing.api.http.dto.cycle.GenerateChargeCycleRequest;
import com.jeepclub.backend.billing.api.http.dto.cycle.GenerateChargeCycleResponse;
import com.jeepclub.backend.billing.api.http.dto.BillingPageSchemas;
import com.jeepclub.backend.billing.core.application.result.cycle.ChargeCycleResult;
import com.jeepclub.backend.billing.core.application.result.cycle.GenerateChargeCycleResult;
import com.jeepclub.backend.billing.core.application.service.chargecycle.AdminChargeCycleService;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.openapi.security.RequiredPermission;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Billing - Charge Cycles",
        description = "Endpoints administrativos para geração, consulta e encerramento de ciclos de cobrança."
)
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Permissão administrativa ausente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class AdminChargeCycleController {

    private final AdminChargeCycleService adminChargeCycleService;

    @PostMapping("/billing/charge-definitions/{chargeDefinitionId}/cycles")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_CYCLE_GENERATE')")
    @RequiredPermission("BILLING_CHARGE_CYCLE_GENERATE")
    @ApiResponse(responseCode = "400", description = "Payload inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Definição não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Definição inativa, ciclo duplicado ou nenhum usuário elegível resolvido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Gerar ciclo de cobrança",
            description = "Gera um ciclo para uma definição de cobrança e cria os débitos dos membros elegíveis."
    )
    public ResponseEntity<GenerateChargeCycleResponse> generate(
            @PathVariable @Positive(message = "ID da definição de cobrança deve ser maior que zero.") Long chargeDefinitionId,
            @Valid @RequestBody GenerateChargeCycleRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        GenerateChargeCycleResult result = adminChargeCycleService.generate(
                chargeDefinitionId,
                request.code(),
                request.dueDate(),
                principal.getUserId()
        );

        return ResponseEntity
                .created(URI.create("/billing/charge-cycles/" + result.chargeCycle().id()))
                .body(GenerateChargeCycleResponse.from(result));
    }

    @GetMapping("/billing/charge-definitions/{chargeDefinitionId}/cycles")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_CYCLE_READ')")
    @RequiredPermission("BILLING_CHARGE_CYCLE_READ")
    @ApiResponse(responseCode = "404", description = "Definição não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Listar ciclos de uma definição de cobrança",
            description = "Lista os ciclos gerados para a definição usando page zero-based, size 20 por padrão e limite global de 50.",
            responses = @ApiResponse(responseCode = "200", description = "Página de ciclos retornada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BillingPageSchemas.ChargeCycles.class)))
    )
    public ResponseEntity<Page<ChargeCycleSummaryResponse>> findByChargeDefinitionId(
            @PathVariable @Positive(message = "ID da definição de cobrança deve ser maior que zero.") Long chargeDefinitionId,
            @ParameterObject Pageable pageable
    ) {
        Page<ChargeCycleResult> results = adminChargeCycleService.findByChargeDefinitionId(
                chargeDefinitionId,
                pageable
        );

        return ResponseEntity.ok(results.map(ChargeCycleSummaryResponse::from));
    }

    @GetMapping("/billing/charge-cycles/{cycleId}")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_CYCLE_READ')")
    @RequiredPermission("BILLING_CHARGE_CYCLE_READ")
    @ApiResponse(responseCode = "404", description = "Ciclo não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Buscar ciclo de cobrança por ID",
            description = "Consulta os dados de um ciclo de cobrança específico."
    )
    public ResponseEntity<ChargeCycleResponse> findById(
            @PathVariable @Positive(message = "ID do ciclo deve ser maior que zero.") Long cycleId
    ) {
        ChargeCycleResult result = adminChargeCycleService.findById(cycleId);

        return ResponseEntity.ok(ChargeCycleResponse.from(result));
    }

    @PatchMapping("/billing/charge-cycles/{cycleId}/cancel")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_CYCLE_CANCEL')")
    @RequiredPermission("BILLING_CHARGE_CYCLE_CANCEL")
    @ApiResponse(responseCode = "404", description = "Ciclo não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Estado do ciclo não permite cancelamento.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Cancelar ciclo de cobrança",
            description = "Cancela um ciclo de cobrança gerado, cancela cobranças abertas vinculadas e prepara pagamentos elegíveis para reembolso."
    )
    public ResponseEntity<ChargeCycleResponse> cancel(
            @PathVariable @Positive(message = "ID do ciclo deve ser maior que zero.") Long cycleId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ChargeCycleResult result = adminChargeCycleService.cancel(
                cycleId,
                principal.getUserId()
        );

        return ResponseEntity.ok(ChargeCycleResponse.from(result));
    }

    @PatchMapping("/billing/charge-cycles/{cycleId}/finish")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_CYCLE_FINISH')")
    @RequiredPermission("BILLING_CHARGE_CYCLE_FINISH")
    @ApiResponse(responseCode = "404", description = "Ciclo não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Estado do ciclo não permite finalização.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Finalizar ciclo de cobrança",
            description = "Finaliza um ciclo de cobrança gerado sem cancelar cobranças, sem cancelar pagamentos e sem gerar reembolsos."
    )
    public ResponseEntity<ChargeCycleResponse> finish(
            @PathVariable @Positive(message = "ID do ciclo deve ser maior que zero.") Long cycleId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ChargeCycleResult result = adminChargeCycleService.finish(
                cycleId,
                principal.getUserId()
        );

        return ResponseEntity.ok(ChargeCycleResponse.from(result));
    }

    @PatchMapping("/billing/charge-cycles/{cycleId}/archive")
    @PreAuthorize("hasAuthority('BILLING_CHARGE_CYCLE_ARCHIVE')")
    @RequiredPermission("BILLING_CHARGE_CYCLE_ARCHIVE")
    @ApiResponse(responseCode = "404", description = "Ciclo não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Somente ciclos finalizados ou cancelados podem ser arquivados.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Arquivar ciclo de cobrança",
            description = "Arquiva um ciclo finalizado ou cancelado para organização histórica, sem efeito financeiro."
    )
    public ResponseEntity<ChargeCycleResponse> archive(
            @PathVariable @Positive(message = "ID do ciclo deve ser maior que zero.") Long cycleId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ChargeCycleResult result = adminChargeCycleService.archive(
                cycleId,
                principal.getUserId()
        );

        return ResponseEntity.ok(ChargeCycleResponse.from(result));
    }
}
