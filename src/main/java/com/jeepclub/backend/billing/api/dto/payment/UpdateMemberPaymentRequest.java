package com.jeepclub.backend.billing.api.dto.payment;

import com.jeepclub.backend.billing.core.domain.enums.payment.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Dados para atualização de um pagamento enviado pelo membro.")
public record UpdateMemberPaymentRequest(

        @NotNull(message = "Valor pago é obrigatório.")
        @DecimalMin(value = "0.01", message = "Valor pago deve ser maior que zero.")
        @Schema(description = "Valor pago.", example = "250.00", requiredMode = Schema.RequiredMode.REQUIRED)
        BigDecimal amount,

        @NotNull(message = "Método de pagamento é obrigatório.")
        @Schema(description = "Método de pagamento.", example = "PIX", requiredMode = Schema.RequiredMode.REQUIRED)
        PaymentMethod paymentMethod,

        @NotNull(message = "Data do pagamento é obrigatória.")
        @Schema(description = "Data e hora em que o pagamento foi realizado.", requiredMode = Schema.RequiredMode.REQUIRED)
        Instant paidAt,

        @NotNull(message = "Comprovante é obrigatório.")
        @Schema(description = "Arquivo atualizado do comprovante de pagamento.", requiredMode = Schema.RequiredMode.REQUIRED)
        MultipartFile receiptFile,

        @Size(max = 500, message = "Observações devem ter no máximo 500 caracteres.")
        @Schema(description = "Observações do usuário sobre o pagamento.", example = "Comprovante corrigido.", nullable = true)
        String notes
) {
}