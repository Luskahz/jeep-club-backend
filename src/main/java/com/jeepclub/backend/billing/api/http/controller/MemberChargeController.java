package com.jeepclub.backend.billing.api.http.controller;

import com.jeepclub.backend.billing.api.http.dto.charge.MemberChargeResponse;
import com.jeepclub.backend.billing.api.http.dto.charge.MemberChargeSummaryResponse;
import com.jeepclub.backend.billing.api.http.dto.BillingPageSchemas;
import com.jeepclub.backend.billing.core.application.result.charge.MemberChargeResult;
import com.jeepclub.backend.billing.core.application.service.membercharge.MemberChargeService;
import com.jeepclub.backend.billing.core.domain.enums.charge.MemberChargeStatus;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
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
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Billing - Member Charges",
        description = "Endpoints para consulta e gerenciamento de cobranças de membros."
)
@ApiResponses(@ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))))
public class MemberChargeController {

    private final MemberChargeService memberChargeService;

    @GetMapping("/billing/me/member-charges")
    @Operation(
            summary = "Listar minhas cobranças",
            description = "Lista somente as cobranças do usuário autenticado, com filtro opcional pelo status persistido. A paginação usa page zero-based, size 20 por padrão e limite global de 50.",
            responses = @ApiResponse(responseCode = "200", description = "Página de cobranças retornada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BillingPageSchemas.MemberCharges.class)))
    )
    public ResponseEntity<Page<MemberChargeSummaryResponse>> findMine(
            @RequestParam(required = false) MemberChargeStatus status,
            @ParameterObject Pageable pageable,
            Authentication authentication
    ) {
        Long authenticatedUserId = extractUserId(authentication);

        Page<MemberChargeResult> results = memberChargeService.findMine(
                authenticatedUserId,
                status,
                pageable
        );

        return ResponseEntity.ok(results.map(MemberChargeSummaryResponse::from));
    }

    @GetMapping("/billing/me/member-charges/{memberChargeId}")
    @Operation(
            summary = "Buscar minha cobrança por ID",
            description = "Consulta uma cobrança do usuário autenticado, garantindo que ela pertence ao próprio usuário.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Cobrança retornada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MemberChargeResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Cobrança pertence a outro usuário.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Cobrança não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<MemberChargeResponse> findMineById(
            @PathVariable @Positive(message = "ID da cobrança deve ser maior que zero.") Long memberChargeId,
            Authentication authentication
    ) {
        Long authenticatedUserId = extractUserId(authentication);

        MemberChargeResult result = memberChargeService.findMineById(
                authenticatedUserId,
                memberChargeId
        );

        return ResponseEntity.ok(MemberChargeResponse.from(result));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalArgumentException("Authenticated user principal is required.");
        }

        return principal.getUserId();
    }
}
