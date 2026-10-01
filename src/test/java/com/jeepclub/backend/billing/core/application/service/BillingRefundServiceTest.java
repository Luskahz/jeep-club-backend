package com.jeepclub.backend.billing.core.application.service;

import com.jeepclub.backend.billing.core.application.service.memberrefund.*;
import com.jeepclub.backend.billing.core.application.exception.charge.*;
import com.jeepclub.backend.billing.core.application.exception.payment.*;
import com.jeepclub.backend.billing.core.application.exception.refund.*;
import com.jeepclub.backend.billing.core.domain.model.*;
import com.jeepclub.backend.billing.core.domain.enums.refund.*;
import com.jeepclub.backend.billing.core.domain.exception.refund.InvalidMemberRefundStateException;
import com.jeepclub.backend.billing.core.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import java.time.*;
import java.util.*;
import static com.jeepclub.backend.billing.support.BillingFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class BillingRefundServiceTest {
    @Mock MemberRefundRepository refunds;
    @Mock MemberChargeRepository charges;
    @Mock MemberPaymentRepository payments;
    MemberRefundService member;
    AdminMemberRefundService admin;
    @BeforeEach void setup() { member = new MemberRefundService(refunds, charges, payments, CLOCK); admin = new AdminMemberRefundService(refunds, charges, payments, CLOCK); }
    @Test void directRequestUsesPaymentLockAndCreatesFinanciallyLinkedRefund() {
        var p = payment(); p.confirm(99L, NOW); when(payments.findByIdForUpdate(1L)).thenReturn(Optional.of(p));
        when(charges.findById(2L)).thenReturn(Optional.of(charge())); when(refunds.save(any())).thenAnswer(i -> i.getArgument(0));
        var result = member.requestByMemberPaymentId(10L, 1L);
        assertThat(result.status()).isEqualTo(MemberRefundStatus.REQUESTED); assertThat(result.memberPaymentId()).isEqualTo(1L);
        assertThat(result.memberChargeId()).isEqualTo(2L); assertThat(result.chargeCycleId()).isEqualTo(4L); assertThat(result.userId()).isEqualTo(10L);
        assertThat(result.amount()).isEqualByComparingTo(AMOUNT); assertThat(result.requestedAt()).isEqualTo(NOW);
        var order = inOrder(payments, refunds); order.verify(payments).findByIdForUpdate(1L); order.verify(refunds).existsRefundedByMemberPaymentId(1L); order.verify(refunds).findActiveByMemberPaymentId(1L); order.verify(refunds).save(any());
    }
    @ParameterizedTest @ValueSource(ints = {0, 1, 2})
    void existingActiveRefundIsReusedWithoutDuplicatingOrOverwritingReview(int stage) {
        var p = payment(); p.confirm(99L, NOW); var r = eligibility(); if (stage >= 1) r.request(10L, NOW); if (stage == 2) r.approve(99L, NOW);
        when(payments.findByIdForUpdate(1L)).thenReturn(Optional.of(p)); when(charges.findById(2L)).thenReturn(Optional.of(charge()));
        when(refunds.findActiveByMemberPaymentId(1L)).thenReturn(Optional.of(r));
        if (stage == 0) when(refunds.save(r)).thenReturn(r);
        var result = member.requestByMemberPaymentId(10L, 1L);
        assertThat(result.status()).isEqualTo(stage == 2 ? MemberRefundStatus.APPROVED : MemberRefundStatus.REQUESTED);
        if (stage == 0) verify(refunds).save(r); else verify(refunds, never()).save(any());
    }
    @Test void unconfirmedForeignAndAlreadyRefundedPaymentsAreRejected() {
        var p = payment(); when(payments.findByIdForUpdate(1L)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> member.requestByMemberPaymentId(10L, 1L)).isInstanceOf(InvalidRefundPaymentException.class);
        p.confirm(99L, NOW); when(charges.findById(2L)).thenReturn(Optional.of(charge()));
        assertThatThrownBy(() -> member.requestByMemberPaymentId(20L, 1L)).isInstanceOf(MemberChargeAccessDeniedException.class);
        when(refunds.existsRefundedByMemberPaymentId(1L)).thenReturn(true);
        assertThatThrownBy(() -> member.requestByMemberPaymentId(10L, 1L)).isInstanceOf(MemberPaymentAlreadyRefundedException.class);
        verify(refunds, never()).save(any());
    }
    @Test void refundOwnershipAndMissingResourcesReturnSpecificErrors() {
        var r = eligibility(); when(refunds.findById(1L)).thenReturn(Optional.of(r));
        assertThatThrownBy(() -> member.request(1L, 20L)).isInstanceOf(MemberRefundAccessDeniedException.class);
        when(refunds.save(r)).thenReturn(r); member.request(1L, 10L); assertThat(r.isRequested()).isTrue();
        assertThatThrownBy(() -> member.request(404L, 10L)).isInstanceOf(MemberRefundNotFoundException.class);
        assertThatThrownBy(() -> member.requestByMemberPaymentId(10L, 404L)).isInstanceOf(MemberPaymentNotFoundException.class);
        var p = payment(); p.confirm(99L, NOW); when(payments.findByIdForUpdate(1L)).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> member.requestByMemberPaymentId(10L, 1L)).isInstanceOf(MemberChargeNotFoundException.class);
    }
    @ParameterizedTest @ValueSource(ints = {0, 1, 2, 3})
    void lateEligibilityIsOnlyCreatedForConfirmedPaymentWithoutActiveOrCompletedRefund(int outcome) {
        var p = payment(); if (outcome != 0) p.confirm(99L, NOW);
        if (outcome == 2) when(refunds.existsActiveByMemberPaymentId(1L)).thenReturn(true);
        if (outcome == 3) when(refunds.existsRefundedByMemberPaymentId(1L)).thenReturn(true);
        admin = new AdminMemberRefundService(refunds, charges, payments, Clock.offset(CLOCK, Duration.ofDays(31)));
        admin.ensureLateEligibility(charge(), p, 99L, NOW);
        if (outcome == 1) {
            var captor = ArgumentCaptor.forClass(MemberRefund.class); verify(refunds).save(captor.capture()); var r = captor.getValue();
            assertThat(r.getEligibleAt()).isEqualTo(NOW); assertThat(r.getEligibleUntil()).isEqualTo(NOW.plus(Duration.ofDays(30)));
            assertThat(r.getCreatedAt()).isEqualTo(NOW.plus(Duration.ofDays(31)));
            assertThatThrownBy(() -> r.request(10L, NOW.plus(Duration.ofDays(31)))).isInstanceOf(InvalidMemberRefundStateException.class);
        } else verify(refunds, never()).save(any());
    }
    @Test void cycleCancellationCreatesOneEligibilityAndSkipsActiveOrCompletedRefundOnRepeat() {
        var p = payment(); p.confirm(99L, NOW); when(charges.findByChargeCycleId(4L)).thenReturn(List.of(charge()));
        when(payments.findByMemberChargeIdAndStatusIn(eq(2L), any())).thenReturn(List.of(p)); when(payments.findByIdForUpdate(1L)).thenReturn(Optional.of(p));
        assertThat(admin.createEligibilityForCanceledCycle(4L, 99L, NOW)).isEqualTo(1);
        when(refunds.existsActiveByMemberPaymentId(1L)).thenReturn(true); assertThat(admin.createEligibilityForCanceledCycle(4L, 99L, NOW)).isZero();
        when(refunds.existsActiveByMemberPaymentId(1L)).thenReturn(false); when(refunds.existsRefundedByMemberPaymentId(1L)).thenReturn(true);
        assertThat(admin.createEligibilityForCanceledCycle(4L, 99L, NOW)).isZero(); verify(refunds, times(1)).save(any());
    }
    @Test void administrativeCommandsPersistLifecycleAndUseDeterministicClock() {
        var r = request(); when(refunds.findById(1L)).thenReturn(Optional.of(r)); when(refunds.save(any())).thenAnswer(i -> i.getArgument(0));
        admin.approve(1L, 99L); assertThat(r.isApproved()).isTrue(); admin.markAsRefunded(1L, 98L); assertThat(r.isRefunded()).isTrue();
        assertThat(r.getRefundedAt()).isEqualTo(NOW);
        var rejected = request(); when(refunds.findById(1L)).thenReturn(Optional.of(rejected)); admin.reject(1L, 99L, "no"); assertThat(rejected.getStatus()).isEqualTo(MemberRefundStatus.REJECTED);
        var canceled = request(); when(refunds.findById(1L)).thenReturn(Optional.of(canceled)); admin.cancel(1L, 99L); assertThat(canceled.isCanceled()).isTrue();
        var expired = eligibility(); when(refunds.findById(1L)).thenReturn(Optional.of(expired));
        admin = new AdminMemberRefundService(refunds, charges, payments, Clock.offset(CLOCK, Duration.ofDays(30))); admin.expire(1L); assertThat(expired.isExpired()).isTrue();
        assertThatThrownBy(() -> admin.findById(404L)).isInstanceOf(MemberRefundNotFoundException.class);
    }
    @Test void listingsPreserveUserCycleAndStatusScopes() {
        var r = request(); var page = new PageImpl<>(List.of(r)); var pageable = Pageable.unpaged();
        when(refunds.findByUserId(10L, pageable)).thenReturn(page); when(refunds.findByChargeCycleId(4L, pageable)).thenReturn(page);
        when(refunds.findAll(pageable)).thenReturn(page); when(refunds.findByStatus(MemberRefundStatus.REQUESTED, pageable)).thenReturn(page); when(refunds.findById(1L)).thenReturn(Optional.of(r));
        assertThat(member.findByUserId(10L, pageable).getContent()).hasSize(1); assertThat(admin.findByChargeCycleId(4L, pageable).getContent()).hasSize(1);
        assertThat(admin.findAll(null, pageable).getContent()).hasSize(1); assertThat(admin.findAll(MemberRefundStatus.REQUESTED, pageable).getContent()).hasSize(1);
        assertThat(admin.findById(1L).status()).isEqualTo(MemberRefundStatus.REQUESTED);
    }
}
