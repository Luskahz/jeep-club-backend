package com.jeepclub.backend.billing.api.http.controller;

import com.jeepclub.backend.billing.api.http.dto.payment.MemberPaymentResponse;
import com.jeepclub.backend.billing.api.http.dto.payment.SubmitMemberPaymentRequest;
import com.jeepclub.backend.billing.api.http.dto.payment.UpdateMemberPaymentRequest;
import com.jeepclub.backend.billing.core.application.exception.payment.InvalidPaymentReceiptException;
import com.jeepclub.backend.billing.core.application.result.MemberPaymentResult;
import com.jeepclub.backend.billing.core.application.service.memberpayment.MemberPaymentService;
import com.jeepclub.backend.billing.core.port.payment.PaymentReceiptFile;
import com.jeepclub.backend.platform.security.principal.UserPrincipal;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;

@RestController
@RequiredArgsConstructor
@Validated
@Tag(
        name = "Billing - Member Payments",
        description = "Endpoints para envio, atualização, consulta e validação de pagamentos de membros."
)
@ApiResponses(@ApiResponse(responseCode = "401", description = "Usuário não autenticado.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))))
public class MemberPaymentController {

    private final MemberPaymentService memberPaymentService;

    @PostMapping(
            value = "/billing/member-charges/{memberChargeId}/payments",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Enviar comprovante de pagamento",
            description = "Recebe multipart/form-data com amount, paymentMethod, paidAt, receiptFile obrigatório e notes opcional. A cobrança deve pertencer ao usuário, aceitar pagamento na data atual e não possuir outro pagamento editável.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Pagamento enviado para validação.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MemberPaymentResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Payload, valor ou comprovante inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Cobrança pertence a outro usuário.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Cobrança não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Cobrança não aceita pagamento ou já possui pagamento editável.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<MemberPaymentResponse> submitForValidation(
            @PathVariable @Positive(message = "ID da cobrança deve ser maior que zero.") Long memberChargeId,
            @Valid @ModelAttribute SubmitMemberPaymentRequest request,
            Authentication authentication
    ) {
        Long authenticatedUserId = extractUserId(authentication);
        PaymentReceiptFile receiptFile = toPaymentReceiptFile(request.receiptFile());

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

    @PutMapping(
            value = "/billing/member-payments/{paymentId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(
            summary = "Atualizar comprovante de pagamento",
            description = "Substitui integralmente os dados e o receiptFile de um pagamento próprio PENDING_VALIDATION ou REJECTED. Pagamento rejeitado volta a PENDING_VALIDATION; nesse caso a janela de pagamento é revalidada.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Pagamento atualizado e novo comprovante associado.", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = MemberPaymentResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Payload, valor ou comprovante inválido.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Pagamento pertence a cobrança de outro usuário.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Pagamento ou cobrança vinculada não encontrada.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Estado do pagamento ou da cobrança não permite atualização.", content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE, schema = @Schema(implementation = ApiErrorResponse.class)))
            }
    )
    public ResponseEntity<MemberPaymentResponse> updateSubmission(
            @PathVariable @Positive(message = "ID do pagamento deve ser maior que zero.") Long paymentId,
            @Valid @ModelAttribute UpdateMemberPaymentRequest request,
            Authentication authentication
    ) {
        Long authenticatedUserId = extractUserId(authentication);
        PaymentReceiptFile receiptFile = toPaymentReceiptFile(request.receiptFile());

        MemberPaymentResult result = memberPaymentService.updateSubmission(
                authenticatedUserId,
                paymentId,
                request.amount(),
                request.paymentMethod(),
                request.paidAt(),
                receiptFile,
                request.notes()
        );

        return ResponseEntity.ok(MemberPaymentResponse.from(result));
    }

    private PaymentReceiptFile toPaymentReceiptFile(MultipartFile multipartFile) {
        if (multipartFile == null) {
            return null;
        }

        try {
            return new PaymentReceiptFile(
                    multipartFile.getOriginalFilename(),
                    multipartFile.getContentType(),
                    multipartFile.getBytes()
            );
        } catch (IOException exception) {
            throw new InvalidPaymentReceiptException("Could not read payment receipt file.");
        }
    }

    private Long extractUserId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new IllegalArgumentException("Authenticated user principal is required.");
        }

        return principal.getUserId();
    }
}
