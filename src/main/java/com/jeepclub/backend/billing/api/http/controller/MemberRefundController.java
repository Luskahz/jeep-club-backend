package com.jeepclub.backend.billing.api.http.controller;

import com.jeepclub.backend.billing.api.http.dto.refund.MemberRefundResponse;
import com.jeepclub.backend.billing.api.http.dto.refund.MemberRefundSummaryResponse;
import com.jeepclub.backend.billing.core.application.result.MemberRefundResult;
import com.jeepclub.backend.billing.core.application.service.memberrefund.MemberRefundService;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Billing - Member Refunds",
        description = "Endpoints para consulta e gestão de reembolsos de membros."
)
public class MemberRefundController {

    private final MemberRefundService memberRefundService;

    @PostMapping("/billing/member-payments/{paymentId}/refund-request")
    @Operation(
            summary = "Solicitar reembolso de um pagamento",
            description = "Permite que o usuário autenticado solicite reembolso de um pagamento próprio confirmado ou pendente de validação."
    )
    public ResponseEntity<MemberRefundResponse> requestByMemberPaymentId(
            @PathVariable @Positive(message = "ID do pagamento deve ser maior que zero.") Long paymentId,
            Authentication authentication
    ) {
        MemberRefundResult result = memberRefundService.requestByMemberPaymentId(
                extractUserId(authentication),
                paymentId
        );
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
        Page<MemberRefundResult> results = memberRefundService.findByUserId(
                extractUserId(authentication),
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
        MemberRefundResult result = memberRefundService.request(
                refundId,
                extractUserId(authentication)
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
