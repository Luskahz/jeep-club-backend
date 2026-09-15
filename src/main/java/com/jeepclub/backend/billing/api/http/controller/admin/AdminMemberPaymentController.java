package com.jeepclub.backend.billing.api.http.controller.admin;

import com.jeepclub.backend.billing.api.http.dto.payment.MemberPaymentResponse;
import com.jeepclub.backend.billing.api.http.dto.payment.MemberPaymentSummaryResponse;
import com.jeepclub.backend.billing.api.http.dto.payment.RejectMemberPaymentRequest;
import com.jeepclub.backend.billing.api.http.dto.BillingPageSchemas;
import com.jeepclub.backend.billing.core.application.result.MemberPaymentResult;
import com.jeepclub.backend.billing.core.application.service.memberpayment.AdminMemberPaymentService;
import com.jeepclub.backend.billing.core.domain.enums.payment.MemberPaymentStatus;
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
        name = "Billing - Member Payments",
        description = "Endpoints para envio, atualização, consulta e validação de pagamentos de membros."
)
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "403", description = "Permissão administrativa ausente.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
})
public class AdminMemberPaymentController {

    private final AdminMemberPaymentService adminMemberPaymentService;

    @GetMapping("/billing/member-payments")
    @PreAuthorize("hasAuthority('BILLING_PAYMENT_READ')")
    @RequiredPermission("BILLING_PAYMENT_READ")
    @Operation(
            summary = "Listar pagamentos de membros",
            description = "Lista pagamentos com filtro opcional por status. Usa page zero-based, size 20 por padrão e limite global de 50.",
            responses = @ApiResponse(responseCode = "200", description = "Página de pagamentos retornada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BillingPageSchemas.MemberPayments.class)))
    )
    public ResponseEntity<Page<MemberPaymentSummaryResponse>> findAll(
            @RequestParam(required = false) MemberPaymentStatus status,
            @ParameterObject Pageable pageable
    ) {
        Page<MemberPaymentResult> results = adminMemberPaymentService.findAll(status, pageable);
        return ResponseEntity.ok(results.map(MemberPaymentSummaryResponse::from));
    }

    @GetMapping("/billing/member-payments/{paymentId}")
    @PreAuthorize("hasAuthority('BILLING_PAYMENT_READ')")
    @RequiredPermission("BILLING_PAYMENT_READ")
    @ApiResponse(responseCode = "404", description = "Pagamento não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Buscar pagamento por ID",
            description = "Consulta os dados completos de um pagamento de membro."
    )
    public ResponseEntity<MemberPaymentResponse> findById(
            @PathVariable @Positive(message = "ID do pagamento deve ser maior que zero.") Long paymentId
    ) {
        MemberPaymentResult result = adminMemberPaymentService.findById(paymentId);
        return ResponseEntity.ok(MemberPaymentResponse.from(result));
    }

    @PatchMapping("/billing/member-payments/{paymentId}/confirm")
    @PreAuthorize("hasAuthority('BILLING_PAYMENT_CONFIRM')")
    @RequiredPermission("BILLING_PAYMENT_CONFIRM")
    @ApiResponse(responseCode = "404", description = "Pagamento ou cobrança vinculada não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Pagamento não está pendente de validação ou cobrança não pode ser quitada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Confirmar pagamento",
            description = "Confirma um pagamento pendente de validação e marca a cobrança vinculada como paga."
    )
    public ResponseEntity<MemberPaymentResponse> confirm(
            @PathVariable @Positive(message = "ID do pagamento deve ser maior que zero.") Long paymentId,
            Authentication authentication
    ) {
        MemberPaymentResult result = adminMemberPaymentService.confirm(
                paymentId,
                extractUserId(authentication)
        );
        return ResponseEntity.ok(MemberPaymentResponse.from(result));
    }

    @PatchMapping("/billing/member-payments/{paymentId}/reject")
    @PreAuthorize("hasAuthority('BILLING_PAYMENT_REJECT')")
    @RequiredPermission("BILLING_PAYMENT_REJECT")
    @ApiResponse(responseCode = "400", description = "Motivo de rejeição inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Pagamento não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Pagamento não está pendente de validação.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
    @Operation(
            summary = "Rejeitar pagamento",
            description = "Rejeita um pagamento pendente de validação, mantendo a cobrança vinculada em aberto."
    )
    public ResponseEntity<MemberPaymentResponse> reject(
            @PathVariable @Positive(message = "ID do pagamento deve ser maior que zero.") Long paymentId,
            @Valid @RequestBody RejectMemberPaymentRequest request,
            Authentication authentication
    ) {
        MemberPaymentResult result = adminMemberPaymentService.reject(
                paymentId,
                extractUserId(authentication),
                request.rejectionReason()
        );
        return ResponseEntity.ok(MemberPaymentResponse.from(result));
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalArgumentException("Authenticated user principal is required.");
        }
        return principal.getUserId();
    }
}
