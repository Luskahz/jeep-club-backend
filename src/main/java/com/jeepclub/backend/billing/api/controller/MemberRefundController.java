package com.jeepclub.backend.billing.api.controller;

import com.jeepclub.backend.billing.api.dto.refund.MemberRefundResponse;
import com.jeepclub.backend.billing.api.dto.refund.MemberRefundSummaryResponse;
import com.jeepclub.backend.billing.api.dto.refund.RejectMemberRefundRequest;
import com.jeepclub.backend.billing.core.application.result.MemberRefundResult;
import com.jeepclub.backend.billing.core.application.service.MemberRefundService;
import com.jeepclub.backend.billing.core.domain.enums.refund.MemberRefundStatus;
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
        name = "Billing - Member Refunds",
        description = "Endpoints para consulta e gestão de reembolsos de membros."
)
public class MemberRefundController {

    private final MemberRefundService memberRefundService;

    @GetMapping("/billing/member-refunds")
    @PreAuthorize("hasAuthority('BILLING_REFUND_READ')")
    @Operation(
            summary = "Listar reembolsos de membros",
            description = "Lista reembolsos de membros de forma paginada, com filtro opcional por status."
    )
    public ResponseEntity<Page<MemberRefundSummaryResponse>> findAll(
            @RequestParam(required = false) MemberRefundStatus status,
            @ParameterObject Pageable pageable
    ) {
        Page<MemberRefundResult> results = memberRefundService.findAll(
                status,
                pageable
        );

        return ResponseEntity.ok(results.map(MemberRefundSummaryResponse::from));
    }

    @GetMapping("/billing/member-refunds/{refundId}")
    @PreAuthorize("hasAuthority('BILLING_REFUND_READ')")
    @Operation(
            summary = "Buscar reembolso por ID",
            description = "Consulta os dados completos de um reembolso de membro."
    )
    public ResponseEntity<MemberRefundResponse> findById(
            @PathVariable @Positive(message = "ID do reembolso deve ser maior que zero.") Long refundId
    ) {
        MemberRefundResult result = memberRefundService.findById(refundId);

        return ResponseEntity.ok(MemberRefundResponse.from(result));
    }

    @GetMapping("/billing/users/me/member-refunds")
    @Operation(
            summary = "Listar meus reembolsos",
            description = "Lista os reembolsos vinculados ao usuário autenticado."
    )
    public ResponseEntity<Page<MemberRefundSummaryResponse>> findMyRefunds(
            @ParameterObject Pageable pageable,
            Authentication authentication
    ) {
        Long authenticatedUserId = extractUserId(authentication);

        Page<MemberRefundResult> results = memberRefundService.findByUserId(
                authenticatedUserId,
                pageable
        );

        return ResponseEntity.ok(results.map(MemberRefundSummaryResponse::from));
    }

    @GetMapping("/billing/charge-cycles/{cycleId}/member-refunds")
    @PreAuthorize("hasAuthority('BILLING_REFUND_READ')")
    @Operation(
            summary = "Listar reembolsos de um ciclo",
            description = "Lista os reembolsos vinculados a um ciclo de cobrança."
    )
    public ResponseEntity<Page<MemberRefundSummaryResponse>> findByChargeCycleId(
            @PathVariable @Positive(message = "ID do ciclo deve ser maior que zero.") Long cycleId,
            @ParameterObject Pageable pageable
    ) {
        Page<MemberRefundResult> results = memberRefundService.findByChargeCycleId(
                cycleId,
                pageable
        );

        return ResponseEntity.ok(results.map(MemberRefundSummaryResponse::from));
    }

    @PatchMapping("/billing/member-refunds/{refundId}/request")
    @Operation(
            summary = "Solicitar reembolso elegível",
            description = "Permite que o usuário autenticado solicite um reembolso que já está elegível."
    )
    public ResponseEntity<MemberRefundResponse> request(
            @PathVariable @Positive(message = "ID do reembolso deve ser maior que zero.") Long refundId,
            Authentication authentication
    ) {
        Long requestedByUserId = extractUserId(authentication);

        MemberRefundResult result = memberRefundService.request(
                refundId,
                requestedByUserId
        );

        return ResponseEntity.ok(MemberRefundResponse.from(result));
    }

    @PatchMapping("/billing/member-refunds/{refundId}/approve")
    @PreAuthorize("hasAuthority('BILLING_REFUND_APPROVE')")
    @Operation(
            summary = "Aprovar reembolso",
            description = "Aprova um reembolso elegível ou solicitado."
    )
    public ResponseEntity<MemberRefundResponse> approve(
            @PathVariable @Positive(message = "ID do reembolso deve ser maior que zero.") Long refundId,
            Authentication authentication
    ) {
        Long approvedByUserId = extractUserId(authentication);

        MemberRefundResult result = memberRefundService.approve(
                refundId,
                approvedByUserId
        );

        return ResponseEntity.ok(MemberRefundResponse.from(result));
    }

    @PatchMapping("/billing/member-refunds/{refundId}/reject")
    @PreAuthorize("hasAuthority('BILLING_REFUND_REJECT')")
    @Operation(
            summary = "Rejeitar reembolso",
            description = "Rejeita um reembolso solicitado, registrando o motivo da rejeição."
    )
    public ResponseEntity<MemberRefundResponse> reject(
            @PathVariable @Positive(message = "ID do reembolso deve ser maior que zero.") Long refundId,
            @Valid @RequestBody RejectMemberRefundRequest request,
            Authentication authentication
    ) {
        Long rejectedByUserId = extractUserId(authentication);

        MemberRefundResult result = memberRefundService.reject(
                refundId,
                rejectedByUserId,
                request.rejectionReason()
        );

        return ResponseEntity.ok(MemberRefundResponse.from(result));
    }

    @PatchMapping("/billing/member-refunds/{refundId}/mark-as-refunded")
    @PreAuthorize("hasAuthority('BILLING_REFUND_MARK_AS_REFUNDED')")
    @Operation(
            summary = "Marcar reembolso como realizado",
            description = "Marca um reembolso aprovado como efetivamente realizado."
    )
    public ResponseEntity<MemberRefundResponse> markAsRefunded(
            @PathVariable @Positive(message = "ID do reembolso deve ser maior que zero.") Long refundId,
            Authentication authentication
    ) {
        Long refundedByUserId = extractUserId(authentication);

        MemberRefundResult result = memberRefundService.markAsRefunded(
                refundId,
                refundedByUserId
        );

        return ResponseEntity.ok(MemberRefundResponse.from(result));
    }

    @PatchMapping("/billing/member-refunds/{refundId}/expire")
    @PreAuthorize("hasAuthority('BILLING_REFUND_EXPIRE')")
    @Operation(
            summary = "Expirar elegibilidade de reembolso",
            description = "Expira manualmente um reembolso elegível quando sua janela de elegibilidade já passou."
    )
    public ResponseEntity<MemberRefundResponse> expire(
            @PathVariable @Positive(message = "ID do reembolso deve ser maior que zero.") Long refundId
    ) {
        MemberRefundResult result = memberRefundService.expire(refundId);

        return ResponseEntity.ok(MemberRefundResponse.from(result));
    }

    @PatchMapping("/billing/member-refunds/{refundId}/cancel")
    @PreAuthorize("hasAuthority('BILLING_REFUND_CANCEL')")
    @Operation(
            summary = "Cancelar processo de reembolso",
            description = "Cancela um processo de reembolso ainda ativo."
    )
    public ResponseEntity<MemberRefundResponse> cancel(
            @PathVariable @Positive(message = "ID do reembolso deve ser maior que zero.") Long refundId,
            Authentication authentication
    ) {
        Long canceledByUserId = extractUserId(authentication);

        MemberRefundResult result = memberRefundService.cancel(
                refundId,
                canceledByUserId
        );

        return ResponseEntity.ok(MemberRefundResponse.from(result));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalArgumentException("Authenticated user principal is required.");
        }

        return principal.getUserId();
    }
}