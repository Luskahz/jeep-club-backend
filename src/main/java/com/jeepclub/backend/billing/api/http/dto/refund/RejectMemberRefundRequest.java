package com.jeepclub.backend.billing.api.http.dto.refund;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para rejeição de reembolso.")
public record RejectMemberRefundRequest(

        @NotBlank(message = "Motivo da rejeição é obrigatório.")
        @Size(max = 500, message = "Motivo da rejeição deve ter no máximo 500 caracteres.")
        @Schema(
                description = "Motivo da rejeição do reembolso.",
                example = "Reembolso negado porque o pagamento não foi identificado na conta do clube.",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String rejectionReason
) {
}