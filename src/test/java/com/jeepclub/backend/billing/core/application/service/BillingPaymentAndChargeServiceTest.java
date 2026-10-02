package com.jeepclub.backend.billing.core.application.service;

import com.jeepclub.backend.billing.core.application.service.membercharge.*;
import com.jeepclub.backend.billing.core.application.service.memberpayment.*;
import com.jeepclub.backend.billing.core.application.service.memberrefund.AdminMemberRefundService;
import com.jeepclub.backend.billing.core.application.service.paymentreceipt.*;
import com.jeepclub.backend.billing.core.application.exception.charge.*;
import com.jeepclub.backend.billing.core.application.exception.payment.*;
import com.jeepclub.backend.billing.core.domain.exception.payment.InvalidMemberPaymentStateException;
import com.jeepclub.backend.billing.core.domain.enums.charge.*;
import com.jeepclub.backend.billing.core.domain.enums.payment.*;
import com.jeepclub.backend.billing.core.domain.model.*;
import com.jeepclub.backend.billing.core.port.payment.PaymentReceiptFile;
import com.jeepclub.backend.billing.core.repository.*;
import com.jeepclub.backend.shared.storage.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static com.jeepclub.backend.billing.support.BillingFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class BillingPaymentAndChargeServiceTest {
    @Mock MemberChargeRepository charges;
    @Mock MemberPaymentRepository payments;
    @Mock ChargeCycleRepository cycles;
    @Mock AdminMemberRefundService refunds;
    @Mock PaymentReceiptLifecycle lifecycle;
    @Mock FileStorage storage;
    AdminMemberPaymentService adminPayments;
    AdminMemberChargeService adminCharges;
    MemberChargeService memberCharges;
    MemberPaymentService memberPayments;
    final PaymentReceiptFile file = new PaymentReceiptFile("receipt.pdf", "application/pdf", new byte[]{1});
    @BeforeEach void setup() {
        adminPayments = new AdminMemberPaymentService(payments, charges, CLOCK, cycles, refunds);
        adminCharges = new AdminMemberChargeService(charges, payments, CLOCK);
        memberCharges = new MemberChargeService(charges, CLOCK);
        memberPayments = memberPayments(CLOCK);
    }
    private MemberPaymentService memberPayments(Clock clock) {
        return new MemberPaymentService(payments, charges, new PaymentReceiptValidator(new PaymentReceiptValidationProperties()), lifecycle, storage, clock);
    }
    @Test void confirmationUsesPaymentThenChargeLockAndPaysWithoutRecheckingExpiredWindow() {
        var p = payment(); var c = charge();
        adminPayments = new AdminMemberPaymentService(payments, charges, Clock.offset(CLOCK, Duration.ofDays(1)), cycles, refunds);
        when(payments.findByIdForUpdate(1L)).thenReturn(Optional.of(p)); when(charges.findByIdForUpdate(2L)).thenReturn(Optional.of(c));
        when(cycles.findById(4L)).thenReturn(Optional.of(cycle())); when(payments.save(p)).thenReturn(p);
        var result = adminPayments.confirm(1L, 99L);
        assertThat(result.status()).isEqualTo(MemberPaymentStatus.CONFIRMED); assertThat(c.isPaid()).isTrue();
        assertThat(c.getPaidAt()).isEqualTo(p.getPaidAt()); assertThat(p.getConfirmedAt()).isEqualTo(NOW.plus(Duration.ofDays(1)));
        var order = inOrder(payments, charges); order.verify(payments).findByIdForUpdate(1L); order.verify(charges).findByIdForUpdate(2L);
        order.verify(charges).save(c); order.verify(payments).save(p); verifyNoInteractions(refunds);
    }
    @Test void lateConfirmationPreservesCanceledChargeAndDelegatesEligibilityToCancellationDate() {
        var p = payment(); var c = charge(); c.cancel(NOW); var cycle = cycle(); cycle.cancel(99L, NOW);
        when(payments.findByIdForUpdate(1L)).thenReturn(Optional.of(p)); when(charges.findByIdForUpdate(2L)).thenReturn(Optional.of(c));
        when(cycles.findById(4L)).thenReturn(Optional.of(cycle)); when(payments.save(p)).thenReturn(p);
        adminPayments.confirm(1L, 99L); assertThat(c.isCanceled()).isTrue(); assertThat(p.isConfirmed()).isTrue();
        verify(refunds).ensureLateEligibility(c, p, 99L, NOW);
    }
    @ParameterizedTest @ValueSource(ints = {0, 1, 2})
    void confirmationRejectsClosedChargeOrAmountMismatchWithoutSaving(int conflict) {
        var c = charge(); if (conflict == 0) c.cancel(NOW); if (conflict == 1) c.markAsPaid(NOW, NOW); if (conflict == 2) c.updateFinalAmount(BigDecimal.TEN, DUE, NOW);
        var p = payment(); when(payments.findByIdForUpdate(1L)).thenReturn(Optional.of(p)); when(charges.findByIdForUpdate(2L)).thenReturn(Optional.of(c));
        when(cycles.findById(4L)).thenReturn(Optional.of(cycle()));
        assertThatThrownBy(() -> adminPayments.confirm(1L, 99L)).isInstanceOf(InvalidMemberPaymentStateException.class);
        assertThat(p.isPendingValidation()).isTrue(); verify(payments, never()).save(any()); verify(charges, never()).save(any());
    }
    @Test void rejectionDoesNotMutateDebtAndFinalAmountIsLockedWhilePaymentPending() {
        var p = payment(); when(payments.findByIdForUpdate(1L)).thenReturn(Optional.of(p)); when(payments.save(p)).thenReturn(p);
        adminPayments.reject(1L, 99L, " invalid "); assertThat(p.isRejected()).isTrue(); verifyNoInteractions(charges);
        var c = charge(); when(charges.findByIdForUpdate(2L)).thenReturn(Optional.of(c));
        when(payments.existsByMemberChargeIdAndStatus(2L, MemberPaymentStatus.PENDING_VALIDATION)).thenReturn(true);
        assertThatThrownBy(() -> adminCharges.updateFinalAmount(2L, BigDecimal.TEN)).isInstanceOf(MemberChargeCannotUpdateFinalAmountException.class);
        assertThat(c.getFinalAmount()).isEqualByComparingTo(AMOUNT); verify(charges, never()).save(any());
        when(payments.existsByMemberChargeIdAndStatus(2L, MemberPaymentStatus.PENDING_VALIDATION)).thenReturn(false);
        when(charges.save(c)).thenReturn(c); adminCharges.updateFinalAmount(2L, BigDecimal.TEN); assertThat(c.getFinalAmount()).isEqualByComparingTo(BigDecimal.TEN);
        adminCharges.cancel(2L); assertThat(c.isCanceled()).isTrue();
    }
    @Test void memberReadsAreOwnerScopedAndAdministrativeFilterCombinationsUseCorrectRepository() {
        var c = charge(); var page = new PageImpl<>(List.of(c)); var pageable = Pageable.unpaged();
        when(charges.findById(2L)).thenReturn(Optional.of(c));
        assertThat(memberCharges.findMineById(10L, 2L).userId()).isEqualTo(10L);
        assertThatThrownBy(() -> memberCharges.findMineById(20L, 2L)).isInstanceOf(MemberChargeAccessDeniedException.class);
        assertThatThrownBy(() -> memberCharges.findMineById(10L, 404L)).isInstanceOf(MemberChargeNotFoundException.class);
        when(charges.findByUserId(10L, pageable)).thenReturn(page); when(charges.findByUserIdAndStatus(10L, MemberChargeStatus.PENDING, pageable)).thenReturn(page);
        when(charges.findAll(pageable)).thenReturn(page); when(charges.findByStatus(MemberChargeStatus.PENDING, pageable)).thenReturn(page);
        assertThat(memberCharges.findMine(10L, null, pageable).getContent()).hasSize(1);
        assertThat(memberCharges.findMine(10L, MemberChargeStatus.PENDING, pageable).getContent()).hasSize(1);
        assertThat(adminCharges.findAll(null, null, pageable).getContent()).hasSize(1);
        assertThat(adminCharges.findAll(10L, null, pageable).getContent()).hasSize(1);
        assertThat(adminCharges.findAll(null, MemberChargeStatus.PENDING, pageable).getContent()).hasSize(1);
        assertThat(adminCharges.findAll(10L, MemberChargeStatus.PENDING, pageable).getContent()).hasSize(1);
        assertThat(adminCharges.findById(2L).id()).isEqualTo(2L);
    }
    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 3, 4})
    void invalidSubmissionNeverTouchesStorage(int conflict) {
        var c = charge(); if (conflict == 2) c.cancel(NOW); if (conflict == 3) c.markAsPaid(NOW, NOW);
        when(charges.findByIdForUpdate(2L)).thenReturn(Optional.of(c));
        if (conflict == 1) when(payments.findByMemberChargeIdAndStatusIn(eq(2L), any())).thenReturn(List.of(payment()));
        var expected = conflict == 0 ? MemberChargeAccessDeniedException.class : conflict == 1 ? MemberPaymentAlreadyExistsException.class : conflict == 4 ? InvalidPaymentAmountException.class : InvalidMemberPaymentStateException.class;
        assertThatThrownBy(() -> memberPayments.submitForValidation(conflict == 0 ? 20L : 10L, 2L,
                conflict == 4 ? BigDecimal.TEN : AMOUNT, PaymentMethod.PIX, NOW, file, null)).isInstanceOf(expected);
        verifyNoInteractions(storage, lifecycle); verify(payments, never()).save(any());
    }
    @Test void expiredSubmissionAndRejectedResubmissionFailButPendingReplacementIsAllowed() {
        memberPayments = memberPayments(Clock.offset(CLOCK, Duration.ofDays(1)));
        var c = charge(); var p = payment(); when(charges.findByIdForUpdate(2L)).thenReturn(Optional.of(c));
        assertThatThrownBy(() -> memberPayments.submitForValidation(10L, 2L, AMOUNT, PaymentMethod.PIX, NOW, file, null)).isInstanceOf(InvalidMemberPaymentStateException.class);
        when(payments.findByIdForUpdate(1L)).thenReturn(Optional.of(p));
        p.reject(99L, "bad", NOW);
        assertThatThrownBy(() -> memberPayments.updateSubmission(10L, 1L, AMOUNT, PaymentMethod.PIX, NOW, file, null)).isInstanceOf(InvalidMemberPaymentStateException.class);
        verifyNoInteractions(storage, lifecycle);
        p.updateSubmission(AMOUNT, PaymentMethod.PIX, NOW, "old", null, NOW);
        when(storage.store(any(), any())).thenReturn(new StoredFile("new")); when(payments.save(p)).thenReturn(p);
        memberPayments.updateSubmission(10L, 1L, AMOUNT, PaymentMethod.PIX, NOW, file, null);
        assertThat(p.getSubmittedAt()).isEqualTo(NOW.plus(Duration.ofDays(1))); verify(lifecycle).register("new", "old");
        var order = inOrder(payments, charges, storage, lifecycle); order.verify(payments, atLeastOnce()).findByIdForUpdate(1L);
        order.verify(charges, atLeastOnce()).findByIdForUpdate(2L); order.verify(storage).store(any(), any()); order.verify(lifecycle).register("new", "old"); order.verify(payments).save(p);
    }
    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 3})
    void replacementRevalidatesOwnershipChargeStateAndAmountBeforeStorage(int conflict) {
        var c = charge(); var p = payment();
        if (conflict == 1) c.cancel(NOW);
        if (conflict == 2) c.markAsPaid(NOW, NOW);
        when(payments.findByIdForUpdate(1L)).thenReturn(Optional.of(p));
        when(charges.findByIdForUpdate(2L)).thenReturn(Optional.of(c));
        var expected = conflict == 0 ? MemberChargeAccessDeniedException.class : conflict == 3 ? InvalidPaymentAmountException.class : InvalidMemberPaymentStateException.class;
        assertThatThrownBy(() -> memberPayments.updateSubmission(conflict == 0 ? 20L : 10L, 1L,
                conflict == 3 ? BigDecimal.TEN : AMOUNT, PaymentMethod.PIX, NOW, file, null)).isInstanceOf(expected);
        verifyNoInteractions(storage, lifecycle); verify(payments, never()).save(any());
        assertThat(p.getReceiptStorageKey()).isEqualTo("billing/payment-receipts/private.pdf");
    }
    @Test void memberReplacementAndAdministrativeReadReportMissingReferences() {
        assertThatThrownBy(() -> memberPayments.updateSubmission(10L, 404L, AMOUNT, PaymentMethod.PIX, NOW, file, null))
                .isInstanceOf(MemberPaymentNotFoundException.class);
        when(payments.findByIdForUpdate(1L)).thenReturn(Optional.of(payment()));
        assertThatThrownBy(() -> memberPayments.updateSubmission(10L, 1L, AMOUNT, PaymentMethod.PIX, NOW, file, null))
                .isInstanceOf(MemberChargeNotFoundException.class);
        assertThatThrownBy(() -> adminCharges.findById(404L)).isInstanceOf(MemberChargeNotFoundException.class);
        verifyNoInteractions(storage, lifecycle);
    }
    @Test void missingPaymentAndChargeAreControlledAndPaymentListingsKeepStatusFilter() {
        assertThatThrownBy(() -> adminPayments.findById(404L)).isInstanceOf(MemberPaymentNotFoundException.class);
        assertThatThrownBy(() -> adminPayments.confirm(404L, 99L)).isInstanceOf(MemberPaymentNotFoundException.class);
        assertThatThrownBy(() -> adminCharges.cancel(404L)).isInstanceOf(MemberChargeNotFoundException.class);
        when(payments.findByIdForUpdate(1L)).thenReturn(Optional.of(payment()));
        assertThatThrownBy(() -> adminPayments.confirm(1L, 99L)).isInstanceOf(MemberChargeNotFoundException.class);
        when(payments.findAll(Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(payment())));
        when(payments.findByStatus(MemberPaymentStatus.PENDING_VALIDATION, Pageable.unpaged())).thenReturn(new PageImpl<>(List.of(payment())));
        assertThat(adminPayments.findAll(null, Pageable.unpaged()).getContent()).hasSize(1);
        assertThat(adminPayments.findAll(MemberPaymentStatus.PENDING_VALIDATION, Pageable.unpaged()).getContent()).hasSize(1);
    }
}
