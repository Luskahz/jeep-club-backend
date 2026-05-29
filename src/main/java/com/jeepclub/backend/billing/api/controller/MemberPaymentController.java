package com.jeepclub.backend.billing.api.controller;

import com.jeepclub.backend.billing.api.dto.payment.MemberPaymentResponse;
import com.jeepclub.backend.billing.api.dto.payment.MemberPaymentSummaryResponse;
import com.jeepclub.backend.billing.api.dto.payment.RejectMemberPaymentRequest;
import com.jeepclub.backend.billing.api.dto.payment.SubmitMemberPaymentRequest;
import com.jeepclub.backend.billing.core.application.result.MemberPaymentResult;
import com.jeepclub.backend.billing.core.application.service.MemberPaymentService;
import com.jeepclub.backend.billing.core.domain.enums.payment.MemberPaymentStatus;
import com.jeepclub.backend.billing.core.port.payment.PaymentReceiptFile;
import com.jeepclub.backend.infra.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Billing - Member Payments",
        description = "Endpoints para envio, consulta e validação de pagamentos de membros."
)
public class MemberPaymentController {

    private final MemberPaymentService memberPaymentService;

    @PostMapping(
            value = "/billing/member-charges/{memberChargeId}/payments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Enviar comprovante de pagamento",
            description = "Permite que o usuário autenticado envie o comprovante de pagamento de uma cobrança própria."
    )
    public ResponseEntity<MemberPaymentResponse> submitForValidation(
            @PathVariable @Positive(message = "ID da cobrança deve ser maior que zero.") Long memberChargeId,
            @Valid @ModelAttribute SubmitMemberPaymentRequest request,
            Authentication authentication
    ) throws IOException {
        Long authenticatedUserId = extractUserId(authentication);

        PaymentReceiptFile receiptFile = new PaymentReceiptFile(
                request.receiptFile().getOriginalFilename(),
                request.receiptFile().getContentType(),
                request.receiptFile().getBytes()
        );

        MemberPaymentResult result = memberPaymentService.submitForValidation(
                authenticatedUserId,
                memberChargeId,
                request.amount(),
                request.paymentMethod(),
                request.paidAt(),
                receiptFile,
                request.notes()
        );

        return ResponseEntity
                .created(URI.create("/billing/member-payments/" + result.id()))
                .body(MemberPaymentResponse.from(result));
    }

    @GetMapping("/billing/member-payments")
    @PreAuthorize("hasAuthority('BILLING_PAYMENT_READ')")
    @Operation(
            summary = "Listar pagamentos de membros",
            description = "Lista pagamentos de membros de forma paginada, com filtro opcional por status."
    )
    public ResponseEntity<Page<MemberPaymentSummaryResponse>> findAll(
            @RequestParam(required = false) MemberPaymentStatus status,
            @ParameterObject Pageable pageable
    ) {
        Page<MemberPaymentResult> results = memberPaymentService.findAll(status, pageable);

        return ResponseEntity.ok(results.map(MemberPaymentSummaryResponse::from));
    }

    @GetMapping("/billing/member-payments/{paymentId}")
    @PreAuthorize("hasAuthority('BILLING_PAYMENT_READ')")
    @Operation(
            summary = "Buscar pagamento por ID",
            description = "Consulta os dados completos de um pagamento de membro."
    )
    public ResponseEntity<MemberPaymentResponse> findById(
            @PathVariable @Positive(message = "ID do pagamento deve ser maior que zero.") Long paymentId
    ) {
        MemberPaymentResult result = memberPaymentService.findById(paymentId);

        return ResponseEntity.ok(MemberPaymentResponse.from(result));
    }

    @PatchMapping("/billing/member-payments/{paymentId}/confirm")
    @PreAuthorize("hasAuthority('BILLING_PAYMENT_CONFIRM')")
    @Operation(
            summary = "Confirmar pagamento",
            description = "Confirma um pagamento pendente de validação e marca a cobrança vinculada como paga."
    )
    public ResponseEntity<MemberPaymentResponse> confirm(
            @PathVariable @Positive(message = "ID do pagamento deve ser maior que zero.") Long paymentId,
            Authentication authentication
    ) {
        Long confirmedByUserId = extractUserId(authentication);

        MemberPaymentResult result = memberPaymentService.confirm(
                paymentId,
                confirmedByUserId
        );

        return ResponseEntity.ok(MemberPaymentResponse.from(result));
    }

    @PatchMapping("/billing/member-payments/{paymentId}/reject")
    @PreAuthorize("hasAuthority('BILLING_PAYMENT_REJECT')")
    @Operation(
            summary = "Rejeitar pagamento",
            description = "Rejeita um pagamento pendente de validação, mantendo a cobrança vinculada em aberto."
    )
    public ResponseEntity<MemberPaymentResponse> reject(
            @PathVariable @Positive(message = "ID do pagamento deve ser maior que zero.") Long paymentId,
            @Valid @RequestBody RejectMemberPaymentRequest request,
            Authentication authentication
    ) {
        Long rejectedByUserId = extractUserId(authentication);

        MemberPaymentResult result = memberPaymentService.reject(
                paymentId,
                rejectedByUserId,
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