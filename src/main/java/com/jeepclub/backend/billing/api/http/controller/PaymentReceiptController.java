package com.jeepclub.backend.billing.api.http.controller;

import com.jeepclub.backend.billing.core.application.result.PaymentReceiptResult;
import com.jeepclub.backend.billing.core.application.service.paymentreceipt.PaymentReceiptService;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
import com.jeepclub.backend.platform.web.exception.ApiErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class PaymentReceiptController {

    private final PaymentReceiptService paymentReceiptService;

    @GetMapping("/billing/member-payments/{paymentId}/receipt")
    @Operation(
            summary = "Baixar comprovante de pagamento",
            description = "Permite o download ao dono do pagamento ou a quem possui BILLING_PAYMENT_READ.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Bytes do comprovante, com o Content-Type original suportado.", content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE, schema = @Schema(type = "string", format = "binary"))),
                    @ApiResponse(responseCode = "400", description = "paymentId deve ser maior que zero.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Pagamento não pertence ao usuário e falta permissão administrativa.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Pagamento, cobrança vinculada ou comprovante não encontrado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<Resource> findPaymentReceipt(
            @PathVariable @Positive(message = "ID do pagamento deve ser maior que zero.") Long paymentId,
            Authentication authentication
    ) {
        UserPrincipal principal = extractPrincipal(authentication);
        boolean hasAdministrativeRead = authentication.getAuthorities().stream()
                .anyMatch(authority -> "BILLING_PAYMENT_READ".equals(authority.getAuthority()));
        PaymentReceiptResult receipt = paymentReceiptService.find(
                paymentId,
                principal.getUserId(),
                hasAdministrativeRead
        );

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
                .contentType(MediaType.parseMediaType(receipt.contentType()))
                .contentLength(receipt.size())
                .body(new ByteArrayResource(receipt.content()));
    }

    private static UserPrincipal extractPrincipal(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalArgumentException("Authenticated user principal is required.");
        }
        return principal;
    }
}
