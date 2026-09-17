package com.jeepclub.backend.billing.api.http.controller.admin;

import com.jeepclub.backend.billing.api.http.dto.refund.MemberRefundResponse;
import com.jeepclub.backend.billing.api.http.dto.BillingPageSchemas;
import com.jeepclub.backend.billing.api.http.dto.refund.MemberRefundSummaryResponse;
import com.jeepclub.backend.billing.api.http.dto.refund.RejectMemberRefundRequest;
import com.jeepclub.backend.billing.core.application.result.MemberRefundResult;
import com.jeepclub.backend.billing.core.application.service.memberrefund.AdminMemberRefundService;
import com.jeepclub.backend.billing.core.domain.enums.refund.MemberRefundStatus;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
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
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Billing - Member Refunds",
        description = "Endpoints para consulta e gestão de reembolsos de membros."
)
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Permissão administrativa ausente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class AdminMemberRefundController {

    private final AdminMemberRefundService adminMemberRefundService;

    @GetMapping("/billing/member-refunds")
    @PreAuthorize("hasAuthority('BILLING_REFUND_READ')")
    @RequiredPermission("BILLING_REFUND_READ")
    @Operation(
            summary = "Listar reembolsos de membros",
            description = "Lista reembolsos com filtro opcional por status. Usa page zero-based, size 20 por padrão e limite global de 50.",
            responses = @ApiResponse(responseCode = "200", description = "Página de reembolsos retornada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BillingPageSchemas.MemberRefunds.class)))
    )
    public ResponseEntity<PageResponse<MemberRefundSummaryResponse>> findAll(
            @RequestParam(required = false) MemberRefundStatus status,
            @ParameterObject Pageable pageable
    ) {
        Page<MemberRefundResult> results = adminMemberRefundService.findAll(status, pageable);
        return ResponseEntity.ok(PageResponse.from(results.map(MemberRefundSummaryResponse::from)));
    }

    @GetMapping("/billing/member-refunds/{refundId}")
    @PreAuthorize("hasAuthority('BILLING_REFUND_READ')")
    @RequiredPermission("BILLING_REFUND_READ")
    @ApiResponse(responseCode = "404", description = "Reembolso não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Buscar reembolso por ID",
            description = "Consulta os dados completos de um reembolso de membro."
    )
    public ResponseEntity<MemberRefundResponse> findById(
            @PathVariable @Positive(message = "ID do reembolso deve ser maior que zero.") Long refundId
    ) {
        return ResponseEntity.ok(MemberRefundResponse.from(adminMemberRefundService.findById(refundId)));
    }

    @GetMapping("/billing/charge-cycles/{cycleId}/member-refunds")
    @PreAuthorize("hasAuthority('BILLING_REFUND_READ')")
    @RequiredPermission("BILLING_REFUND_READ")
    @Operation(
            summary = "Listar reembolsos de um ciclo",
            description = "Lista os reembolsos vinculados ao ciclo usando page zero-based, size 20 por padrão e limite global de 50.",
            responses = @ApiResponse(responseCode = "200", description = "Página de reembolsos retornada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BillingPageSchemas.MemberRefunds.class)))
    )
    public ResponseEntity<PageResponse<MemberRefundSummaryResponse>> findByChargeCycleId(
            @PathVariable @Positive(message = "ID do ciclo deve ser maior que zero.") Long cycleId,
            @ParameterObject Pageable pageable
    ) {
        Page<MemberRefundResult> results = adminMemberRefundService.findByChargeCycleId(cycleId, pageable);
        return ResponseEntity.ok(PageResponse.from(results.map(MemberRefundSummaryResponse::from)));
    }

    @PatchMapping("/billing/member-refunds/{refundId}/approve")
    @PreAuthorize("hasAuthority('BILLING_REFUND_APPROVE')")
    @RequiredPermission("BILLING_REFUND_APPROVE")
    @ApiResponse(responseCode = "404", description = "Reembolso não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Estado ou janela de elegibilidade não permite aprovação.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Aprovar reembolso",
            description = "Aprova um reembolso elegível ou solicitado."
    )
    public ResponseEntity<MemberRefundResponse> approve(
            @PathVariable @Positive(message = "ID do reembolso deve ser maior que zero.") Long refundId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(MemberRefundResponse.from(
                adminMemberRefundService.approve(refundId, extractUserId(authentication))
        ));
    }

    @PatchMapping("/billing/member-refunds/{refundId}/reject")
    @PreAuthorize("hasAuthority('BILLING_REFUND_REJECT')")
    @RequiredPermission("BILLING_REFUND_REJECT")
    @ApiResponse(responseCode = "400", description = "Motivo de rejeição inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Reembolso não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Reembolso não está solicitado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Rejeitar reembolso",
            description = "Rejeita um reembolso solicitado, registrando o motivo da rejeição."
    )
    public ResponseEntity<MemberRefundResponse> reject(
            @PathVariable @Positive(message = "ID do reembolso deve ser maior que zero.") Long refundId,
            @Valid @RequestBody RejectMemberRefundRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(MemberRefundResponse.from(
                adminMemberRefundService.reject(
                        refundId,
                        extractUserId(authentication),
                        request.rejectionReason()
                )
        ));
    }

    @PatchMapping("/billing/member-refunds/{refundId}/mark-as-refunded")
    @PreAuthorize("hasAuthority('BILLING_REFUND_MARK_AS_REFUNDED')")
    @RequiredPermission("BILLING_REFUND_MARK_AS_REFUNDED")
    @ApiResponse(responseCode = "404", description = "Reembolso não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Reembolso não está aprovado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Marcar reembolso como realizado",
            description = "Marca um reembolso aprovado como efetivamente realizado."
    )
    public ResponseEntity<MemberRefundResponse> markAsRefunded(
            @PathVariable @Positive(message = "ID do reembolso deve ser maior que zero.") Long refundId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(MemberRefundResponse.from(
                adminMemberRefundService.markAsRefunded(refundId, extractUserId(authentication))
        ));
    }

    @PatchMapping("/billing/member-refunds/{refundId}/expire")
    @PreAuthorize("hasAuthority('BILLING_REFUND_EXPIRE')")
    @RequiredPermission("BILLING_REFUND_EXPIRE")
    @ApiResponse(responseCode = "404", description = "Reembolso não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Reembolso não está elegível ou a janela ainda não expirou.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Expirar elegibilidade de reembolso",
            description = "Expira manualmente um reembolso elegível quando sua janela de elegibilidade já passou."
    )
    public ResponseEntity<MemberRefundResponse> expire(
            @PathVariable @Positive(message = "ID do reembolso deve ser maior que zero.") Long refundId
    ) {
        return ResponseEntity.ok(MemberRefundResponse.from(adminMemberRefundService.expire(refundId)));
    }

    @PatchMapping("/billing/member-refunds/{refundId}/cancel")
    @PreAuthorize("hasAuthority('BILLING_REFUND_CANCEL')")
    @RequiredPermission("BILLING_REFUND_CANCEL")
    @ApiResponse(responseCode = "404", description = "Reembolso não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Reembolso já está em estado terminal.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Cancelar processo de reembolso",
            description = "Cancela um processo de reembolso ainda ativo."
    )
    public ResponseEntity<MemberRefundResponse> cancel(
            @PathVariable @Positive(message = "ID do reembolso deve ser maior que zero.") Long refundId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(MemberRefundResponse.from(
                adminMemberRefundService.cancel(refundId, extractUserId(authentication))
        ));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalArgumentException("Authenticated user principal is required.");
        }
        return principal.getUserId();
    }
}
