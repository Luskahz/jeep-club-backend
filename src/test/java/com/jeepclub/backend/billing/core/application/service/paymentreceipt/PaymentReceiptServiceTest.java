package com.jeepclub.backend.billing.core.application.service.paymentreceipt;

import com.jeepclub.backend.billing.core.application.exception.payment.MemberPaymentAccessDeniedException;
import com.jeepclub.backend.billing.core.application.exception.payment.MemberPaymentNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.payment.PaymentReceiptNotFoundException;
import com.jeepclub.backend.billing.core.domain.enums.charge.MemberChargeStatus;
import com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy;
import com.jeepclub.backend.billing.core.domain.enums.payment.MemberPaymentStatus;
import com.jeepclub.backend.billing.core.domain.enums.payment.PaymentMethod;
import com.jeepclub.backend.billing.core.domain.model.MemberCharge;
import com.jeepclub.backend.billing.core.domain.model.MemberPayment;
import com.jeepclub.backend.billing.core.repository.MemberChargeRepository;
import com.jeepclub.backend.billing.core.repository.MemberPaymentRepository;
import com.jeepclub.backend.shared.storage.FileStorage;
import com.jeepclub.backend.shared.storage.StorageResource;
import com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReceiptServiceTest {

    @Mock MemberPaymentRepository paymentRepository;
    @Mock MemberChargeRepository chargeRepository;
    @Mock FileStorage fileStorage;

    private PaymentReceiptService service;

    @BeforeEach
    void setUp() {
        service = new PaymentReceiptService(paymentRepository, chargeRepository, fileStorage);
    }

    @Test
    void ownerLoadsReceiptOnlyAfterOwnershipCheck() {
        arrangePaymentAndCharge(10L);
        when(fileStorage.load("billing/payment-receipts/file.pdf"))
                .thenReturn(new StorageResource("billing/payment-receipts/file.pdf", new byte[]{1, 2}));

        var result = service.find(1L, 10L, false);

        assertThat(result.content()).containsExactly(1, 2);
        assertThat(result.contentType()).isEqualTo("application/pdf");
        verify(fileStorage).load("billing/payment-receipts/file.pdf");
    }

    @Test
    void administrativePermissionAllowsNonOwner() {
        arrangePaymentAndCharge(10L);
        when(fileStorage.load("billing/payment-receipts/file.pdf"))
                .thenReturn(new StorageResource("billing/payment-receipts/file.pdf", new byte[]{1}));

        assertThat(service.find(1L, 99L, true).content()).containsExactly(1);
    }

    @Test
    void nonOwnerWithoutPermissionIsDeniedBeforeStorageLoad() {
        arrangePaymentAndCharge(10L);

        assertThatThrownBy(() -> service.find(1L, 99L, false))
                .isInstanceOf(MemberPaymentAccessDeniedException.class);
        verify(fileStorage, never()).load(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void missingPaymentDoesNotTouchStorage() {
        when(paymentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.find(404L, 10L, false))
                .isInstanceOf(MemberPaymentNotFoundException.class);
        verify(fileStorage, never()).load(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void missingStoredObjectBecomesControlledBillingNotFound() {
        arrangePaymentAndCharge(10L);
        when(fileStorage.load("billing/payment-receipts/file.pdf"))
                .thenThrow(new StorageObjectNotFoundException());

        assertThatThrownBy(() -> service.find(1L, 10L, false))
                .isInstanceOf(PaymentReceiptNotFoundException.class)
                .hasMessage("Payment receipt file not found.");
    }

    private void arrangePaymentAndCharge(Long ownerId) {
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment()));
        when(chargeRepository.findById(2L)).thenReturn(Optional.of(charge(ownerId)));
    }

    private static MemberPayment payment() {
        Instant now = Instant.parse("2026-09-13T12:00:00Z");
        return MemberPayment.reconstitute(
                1L, 2L, new BigDecimal("100.00"), PaymentMethod.PIX,
                MemberPaymentStatus.PENDING_VALIDATION, now,
                "billing/payment-receipts/file.pdf",
                null, null, null, null, null, null, null, now, null
        );
    }

    private static MemberCharge charge(Long ownerId) {
        return MemberCharge.reconstitute(
                2L, ownerId, 3L, 4L,
                new BigDecimal("100.00"), new BigDecimal("100.00"),
                LocalDate.of(2026, 9, 30), PaymentAcceptancePolicy.UNTIL_DUE_DATE,
                null, LocalDate.of(2026, 9, 30), MemberChargeStatus.PENDING,
                Instant.parse("2026-09-01T12:00:00Z"), null, null, null
        );
    }
}
