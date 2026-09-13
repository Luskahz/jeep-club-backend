package com.jeepclub.backend.billing.core.application.service.memberpayment;

import com.jeepclub.backend.billing.core.application.service.paymentreceipt.PaymentReceiptLifecycle;
import com.jeepclub.backend.billing.core.application.service.paymentreceipt.PaymentReceiptValidationProperties;
import com.jeepclub.backend.billing.core.application.service.paymentreceipt.PaymentReceiptValidator;
import com.jeepclub.backend.billing.core.domain.enums.charge.MemberChargeStatus;
import com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy;
import com.jeepclub.backend.billing.core.domain.enums.payment.MemberPaymentStatus;
import com.jeepclub.backend.billing.core.domain.enums.payment.PaymentMethod;
import com.jeepclub.backend.billing.core.domain.model.MemberCharge;
import com.jeepclub.backend.billing.core.domain.model.MemberPayment;
import com.jeepclub.backend.billing.core.port.payment.PaymentReceiptFile;
import com.jeepclub.backend.billing.core.repository.MemberChargeRepository;
import com.jeepclub.backend.billing.core.repository.MemberPaymentRepository;
import com.jeepclub.backend.shared.storage.FileStorage;
import com.jeepclub.backend.shared.storage.StorageFile;
import com.jeepclub.backend.shared.storage.StoredFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberPaymentServiceReceiptTest {

    private static final Instant NOW = Instant.parse("2026-09-13T12:00:00Z");

    @Mock MemberPaymentRepository paymentRepository;
    @Mock MemberChargeRepository chargeRepository;
    @Mock PaymentReceiptLifecycle lifecycle;
    @Mock FileStorage fileStorage;

    private MemberPaymentService service;

    @BeforeEach
    void setUp() {
        service = new MemberPaymentService(
                paymentRepository,
                chargeRepository,
                new PaymentReceiptValidator(new PaymentReceiptValidationProperties()),
                lifecycle,
                fileStorage,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void submitStoresNormalizedReceiptInRequiredNamespaceAndPersistsReturnedKey() {
        when(chargeRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(charge()));
        when(paymentRepository.findByMemberChargeIdAndStatusIn(any(), any())).thenReturn(List.of());
        when(fileStorage.store(any(), any())).thenReturn(new StoredFile("billing/payment-receipts/B.pdf"));
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.submitForValidation(
                10L, 2L, new BigDecimal("100.00"), PaymentMethod.PIX, NOW,
                new PaymentReceiptFile("receipt.PDF", "APPLICATION/PDF", new byte[]{1}), "notes"
        );

        ArgumentCaptor<StorageFile> file = ArgumentCaptor.forClass(StorageFile.class);
        verify(fileStorage).store(file.capture(), org.mockito.ArgumentMatchers.eq("billing/payment-receipts"));
        assertThat(file.getValue().extension()).isEqualTo("pdf");
        assertThat(file.getValue().contentType()).isEqualTo("application/pdf");
        assertThat(result.receiptStorageKey()).isEqualTo("billing/payment-receipts/B.pdf");
        verify(lifecycle).register("billing/payment-receipts/B.pdf", null);
    }

    @Test
    void updateRegistersSafeReplacementFromOldToNewKey() {
        when(paymentRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(payment()));
        when(chargeRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(charge()));
        when(fileStorage.store(any(), any())).thenReturn(new StoredFile("billing/payment-receipts/B.jpg"));
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.updateSubmission(
                10L, 1L, new BigDecimal("100.00"), PaymentMethod.PIX, NOW,
                new PaymentReceiptFile("new.JPG", "image/jpeg", new byte[]{2}), null
        );

        assertThat(result.receiptStorageKey()).isEqualTo("billing/payment-receipts/B.jpg");
        verify(lifecycle).register(
                "billing/payment-receipts/B.jpg",
                "billing/payment-receipts/A.pdf"
        );
    }

    private static MemberPayment payment() {
        return MemberPayment.reconstitute(
                1L, 2L, new BigDecimal("100.00"), PaymentMethod.PIX,
                MemberPaymentStatus.PENDING_VALIDATION, NOW,
                "billing/payment-receipts/A.pdf",
                null, null, null, null, null, null, null, NOW, null
        );
    }

    private static MemberCharge charge() {
        return MemberCharge.reconstitute(
                2L, 10L, 3L, 4L,
                new BigDecimal("100.00"), new BigDecimal("100.00"),
                LocalDate.of(2026, 9, 30), PaymentAcceptancePolicy.UNTIL_DUE_DATE,
                null, LocalDate.of(2026, 9, 30), MemberChargeStatus.PENDING,
                NOW, null, null, null
        );
    }
}
