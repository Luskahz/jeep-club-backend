package com.jeepclub.backend.billing.infra.persistence.mapper;

import com.jeepclub.backend.billing.api.http.dto.payment.MemberPaymentResponse;
import com.jeepclub.backend.billing.core.application.result.MemberPaymentResult;
import com.jeepclub.backend.billing.core.domain.enums.payment.PaymentMethod;
import com.jeepclub.backend.billing.core.domain.model.MemberPayment;
import com.jeepclub.backend.billing.infra.persistence.entity.MemberPaymentEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class MemberPaymentPersistenceModelTest {

    private final MemberPaymentMapper mapper = new MemberPaymentMapper();

    @Test
    void persistsAndReconstitutesUsingOnlyReceiptStorageKey() {
        Instant now = Instant.parse("2026-09-13T12:00:00Z");
        MemberPayment payment = MemberPayment.submitForValidation(
                10L,
                new BigDecimal("100.00"),
                PaymentMethod.PIX,
                now,
                "billing/payment-receipts/receipt.pdf",
                "notes",
                now
        );

        MemberPaymentEntity entity = mapper.toEntity(payment);
        MemberPayment restored = mapper.toDomain(entity);

        assertThat(entity.getReceiptStorageKey()).isEqualTo("billing/payment-receipts/receipt.pdf");
        assertThat(restored.getReceiptStorageKey()).isEqualTo(entity.getReceiptStorageKey());
        assertThat(Arrays.stream(MemberPaymentEntity.class.getDeclaredFields()).map(java.lang.reflect.Field::getName))
                .doesNotContain("receiptUrl");
    }

    @Test
    void publicResponseHidesStorageKeyAndDerivesLogicalReceiptUrl() {
        Instant now = Instant.parse("2026-09-13T12:00:00Z");
        MemberPayment payment = MemberPayment.reconstitute(
                42L, 10L, new BigDecimal("100.00"), PaymentMethod.PIX,
                com.jeepclub.backend.billing.core.domain.enums.payment.MemberPaymentStatus.PENDING_VALIDATION,
                now, "billing/payment-receipts/private-key.pdf",
                null, null, null, null, null, null, null, now, null
        );

        MemberPaymentResponse response = MemberPaymentResponse.from(MemberPaymentResult.from(payment));

        assertThat(response.receiptUrl()).isEqualTo("/billing/member-payments/42/receipt");
        assertThat(Arrays.stream(MemberPaymentResponse.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName))
                .doesNotContain("receiptStorageKey");
        assertThat(response.toString()).doesNotContain("private-key");
    }
}
