package com.jeepclub.backend.billing.api.http.controller;

import com.jeepclub.backend.billing.api.http.dto.refund.MemberRefundResponse;
import com.jeepclub.backend.billing.api.http.dto.refund.MemberRefundSummaryResponse;
import com.jeepclub.backend.billing.api.http.dto.BillingPageSchemas;
import com.jeepclub.backend.billing.core.application.result.MemberRefundResult;
import com.jeepclub.backend.billing.core.application.service.memberrefund.MemberRefundService;
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
@ApiResponses(@ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))))
public class MemberRefundController {

    private final MemberRefundService memberRefundService;

    @PostMapping("/billing/member-payments/{paymentId}/refund-request")
    @Operation(
            summary = "Solicitar reembolso de um pagamento",
            description = "Solicita reembolso de pagamento próprio CONFIRMED ou PENDING_VALIDATION. Reaproveita elegibilidade ativa existente ou cria solicitação MEMBER_REQUEST.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Reembolso retornado ou solicitado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MemberRefundResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Status do pagamento não permite reembolso.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Pagamento pertence a cobrança de outro usuário.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Pagamento ou cobrança não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Pagamento já foi reembolsado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
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
            description = "Lista somente os reembolsos vinculados ao usuário autenticado. A paginação usa page zero-based, size 20 por padrão e limite global de 50.",
            responses = @ApiResponse(responseCode = "200", description = "Página de reembolsos retornada.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = BillingPageSchemas.MemberRefunds.class)))
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
            description = "Move um reembolso próprio ELIGIBLE e ainda dentro da janela para REQUESTED.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Reembolso solicitado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MemberRefundResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Reembolso pertence a outro usuário.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Reembolso não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Estado ou janela de elegibilidade não permite a solicitação.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
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
