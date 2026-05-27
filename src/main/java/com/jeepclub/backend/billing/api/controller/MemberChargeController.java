package com.jeepclub.backend.billing.api.controller;

import com.jeepclub.backend.billing.api.dto.charge.MemberChargeResponse;
import com.jeepclub.backend.billing.api.dto.charge.MemberChargeSummaryResponse;
import com.jeepclub.backend.billing.api.dto.charge.UpdateMemberChargeFinalAmountRequest;
import com.jeepclub.backend.billing.core.application.result.MemberChargeResult;
import com.jeepclub.backend.billing.core.application.service.MemberChargeService;
import com.jeepclub.backend.billing.core.domain.enums.MemberChargeStatus;
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

@RestController
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Billing - Member Charges",
        description = "Endpoints para consulta e gerenciamento de cobranças de membros."
)
public class MemberChargeController {

    private final MemberChargeService memberChargeService;

    @GetMapping("/billing/member-charges")
    @PreAuthorize("hasAuthority('BILLING_MEMBER_CHARGE_READ')")
    @Operation(
            summary = "Listar cobranças de membros",
            description = "Lista cobranças de membros de forma paginada, com filtros opcionais por usuário e status."
    )
    public ResponseEntity<Page<MemberChargeSummaryResponse>> findAll(
            @RequestParam(required = false) @Positive(message = "ID do usuário deve ser maior que zero.") Long userId,
            @RequestParam(required = false) MemberChargeStatus status,
            @ParameterObject Pageable pageable
    ) {
        Page<MemberChargeResult> results = memberChargeService.findAll(
                userId,
                status,
                pageable
        );

        return ResponseEntity.ok(results.map(MemberChargeSummaryResponse::from));
    }


    @GetMapping("/billing/member-charges/{memberChargeId}")
    @PreAuthorize("hasAuthority('BILLING_MEMBER_CHARGE_READ')")
    @Operation(
            summary = "Buscar cobrança de membro por ID",
            description = "Consulta os dados completos de uma cobrança de membro específica."
    )
    public ResponseEntity<MemberChargeResponse> findById(
            @PathVariable @Positive(message = "ID da cobrança deve ser maior que zero.") Long memberChargeId
    ) {
        MemberChargeResult result = memberChargeService.findById(memberChargeId);

        return ResponseEntity.ok(MemberChargeResponse.from(result));
    }

    @GetMapping("/billing/me/member-charges")
    @Operation(
            summary = "Listar minhas cobranças",
            description = "Lista as cobranças do usuário autenticado de forma paginada."
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
            description = "Consulta uma cobrança do usuário autenticado, garantindo que ela pertence ao próprio usuário."
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

    @PatchMapping("/billing/member-charges/{memberChargeId}/final-amount")
    @PreAuthorize("hasAuthority('BILLING_MEMBER_CHARGE_UPDATE')")
    @Operation(
            summary = "Atualizar valor final da cobrança",
            description = "Atualiza o valor final de uma cobrança aberta, permitindo ajuste administrativo."
    )
    public ResponseEntity<MemberChargeResponse> updateFinalAmount(
            @PathVariable @Positive(message = "ID da cobrança deve ser maior que zero.") Long memberChargeId,
            @Valid @RequestBody UpdateMemberChargeFinalAmountRequest request
    ) {
        MemberChargeResult result = memberChargeService.updateFinalAmount(
                memberChargeId,
                request.finalAmount()
        );

        return ResponseEntity.ok(MemberChargeResponse.from(result));
    }

    @PatchMapping("/billing/member-charges/{memberChargeId}/cancel")
    @PreAuthorize("hasAuthority('BILLING_MEMBER_CHARGE_CANCEL')")
    @Operation(
            summary = "Cancelar cobrança de membro",
            description = "Cancela uma cobrança de membro aberta."
    )
    public ResponseEntity<MemberChargeResponse> cancel(
            @PathVariable @Positive(message = "ID da cobrança deve ser maior que zero.") Long memberChargeId
    ) {
        MemberChargeResult result = memberChargeService.cancel(memberChargeId);

        return ResponseEntity.ok(MemberChargeResponse.from(result));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalArgumentException("Authenticated user principal is required.");
        }

        return principal.getUserId();
    }
}