package com.jeepclub.backend.billing.api.http.dto.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para rejeição de pagamento.")
public record RejectMemberPaymentRequest(

        @NotBlank(message = "Motivo da rejeição é obrigatório.")
        @Size(max = 500, message = "Motivo da rejeição deve ter no máximo 500 caracteres.")
        @Schema(
                description = "Motivo da rejeição do comprovante.",
                example = "Valor do comprovante não confere com a cobrança.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String rejectionReason
) {
}