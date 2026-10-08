package com.jeepclub.backend.billing.infra.persistence.adapter;

import com.jeepclub.backend.billing.core.domain.model.*;
import com.jeepclub.backend.billing.core.domain.model.assignment.*;
import com.jeepclub.backend.billing.core.domain.enums.payment.*;
import com.jeepclub.backend.billing.core.domain.enums.refund.*;
import com.jeepclub.backend.billing.core.repository.*;
import com.jeepclub.backend.billing.core.application.service.memberpayment.AdminMemberPaymentService;
import com.jeepclub.backend.billing.core.application.service.memberrefund.AdminMemberRefundService;
import com.jeepclub.backend.billing.core.domain.exception.payment.InvalidMemberPaymentStateException;
import com.jeepclub.backend.billing.infra.persistence.mapper.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.transaction.*;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.annotation.*;
import org.springframework.data.domain.*;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.util.*;
import java.util.concurrent.*;
import static com.jeepclub.backend.billing.support.BillingFixtures.*;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false)
@org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase(replace = org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE)
@org.springframework.test.context.ActiveProfiles("test")
@Import({ChargeDefinitionAdapter.class, ChargeAssignmentAdapter.class, ChargeCycleAdapter.class, MemberChargeAdapter.class,
        MemberPaymentAdapter.class, MemberRefundAdapter.class, ChargeDefinitionMapper.class, ChargeAssignmentMapper.class,
        ChargeCycleMapper.class, MemberChargeMapper.class, MemberPaymentMapper.class, MemberRefundMapper.class,
        AdminMemberPaymentService.class, AdminMemberRefundService.class, BillingPersistenceLifecycleTest.TimeConfiguration.class})
class BillingPersistenceLifecycleTest {
    @Autowired ChargeDefinitionRepository definitions;
    @Autowired ChargeAssignmentRepository assignments;
    @Autowired ChargeCycleRepository cycles;
    @Autowired MemberChargeRepository charges;
    @Autowired MemberPaymentRepository payments;
    @Autowired MemberRefundRepository refunds;
    @Autowired AdminMemberPaymentService administration;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired EntityManager em;
    @TestConfiguration static class TimeConfiguration { @Bean Clock billingClock() { return CLOCK; } }
    private ChargeDefinition newDefinition() {
        return ChargeDefinition.create("fee", null, AMOUNT, com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType.ONE_TIME,
                true, com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy.AFTER_DUE_DATE, null, NOW);
    }
    @Test void allAssignmentSubtypesRoundTripWithLifecycleAndTargetIdentity() {
        var input = List.of(AllMembersChargeAssignment.create(3L, NOW), UserChargeAssignment.create(3L, 10L, NOW),
                RoleChargeAssignment.create(3L, 20L, NOW), EventParticipantsChargeAssignment.create(3L, 30L, NOW));
        for (var a : input) {
            var saved = assignments.save(a); em.flush(); em.clear();
            var restored = assignments.findById(saved.getId()).orElseThrow();
            assertThat(restored).usingRecursiveComparison().isEqualTo(saved);
            restored.deactivate(NOW.plusSeconds(1)); assignments.save(restored); em.flush(); em.clear();
            assertThat(assignments.findById(saved.getId()).orElseThrow().isActive()).isFalse();
        }
        assertThat(assignments.existsAllMembersAssignmentByChargeDefinitionId(3L)).isTrue();
        assertThat(assignments.existsUserAssignmentByChargeDefinitionIdAndUserId(3L, 10L)).isTrue();
        assertThat(assignments.existsRoleAssignmentByChargeDefinitionIdAndRoleId(3L, 20L)).isTrue();
        assertThat(assignments.existsEventParticipantsAssignmentByChargeDefinitionIdAndEventId(3L, 30L)).isTrue();
    }
    @Test void historicalCycleAndEveryRefundStateSurviveFlushAndReconstitution() {
        var d = definitions.save(newDefinition()); var c = cycles.save(ChargeCycle.generate(d, "HISTORY", DUE, 99L, NOW));
        c.cancel(99L, NOW.plusSeconds(1)); c.archive(98L, NOW.plusSeconds(2)); var saved = cycles.save(c);
        em.flush(); em.clear(); assertThat(cycles.findById(saved.getId()).orElseThrow()).usingRecursiveComparison().isEqualTo(saved);
        for (var state : MemberRefundStatus.values()) {
            var r = eligibility();
            switch (state) {
                case REQUESTED -> r.request(10L, NOW);
                case APPROVED -> r.approve(99L, NOW);
                case REFUNDED -> { r.approve(99L, NOW); r.markAsRefunded(98L, NOW); }
                case REJECTED -> { r.request(10L, NOW); r.reject(99L, "reason", NOW); }
                case EXPIRED -> r.expire(NOW.plusSeconds(30 * 86400));
                case CANCELED -> r.cancel(99L, NOW);
                default -> { }
            }
            var stored = refunds.save(r); em.flush(); em.clear();
            assertThat(refunds.findById(stored.getId()).orElseThrow()).usingRecursiveComparison().isEqualTo(stored);
        }
        assertThat(refunds.existsActiveByMemberPaymentId(1L)).isTrue(); assertThat(refunds.existsRefundedByMemberPaymentId(1L)).isTrue();
        assertThat(refunds.findByStatus(MemberRefundStatus.REFUNDED, Pageable.unpaged()).getTotalElements()).isEqualTo(1);
    }
    @Test @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void twoConcurrentConfirmationsSerializeAndOnlyOneCanPayTheDebt() throws Exception {
        var tx = new TransactionTemplate(transactionManager);
        var ids = tx.execute(status -> {
            var d = definitions.save(newDefinition()); var c = cycles.save(ChargeCycle.generate(d, "CONCURRENT", DUE, 99L, NOW));
            var debt = charges.save(MemberCharge.create(10L, d.getId(), c.getId(), AMOUNT, DUE,
                    com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy.AFTER_DUE_DATE, null, NOW));
            var payment = payments.save(MemberPayment.submitForValidation(debt.getId(), AMOUNT, PaymentMethod.PIX, NOW, "private-key", null, NOW));
            return List.of(payment.getId(), debt.getId(), c.getId(), d.getId());
        });
        var executor = Executors.newFixedThreadPool(2); var ready = new CountDownLatch(2); var start = new CountDownLatch(1);
        Callable<Boolean> confirm = () -> { ready.countDown(); if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("start barrier timed out");
            try { administration.confirm(ids.get(0), 99L); return true; } catch (InvalidMemberPaymentStateException expected) { return false; } };
        try {
            var first = executor.submit(confirm); var second = executor.submit(confirm);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue(); start.countDown();
            assertThat(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
            tx.executeWithoutResult(status -> {
                assertThat(payments.findById(ids.get(0)).orElseThrow().isConfirmed()).isTrue();
                assertThat(charges.findById(ids.get(1)).orElseThrow().isPaid()).isTrue();
                assertThat(refunds.existsActiveByMemberPaymentId(ids.get(0))).isFalse();
            });
        } finally {
            start.countDown(); executor.shutdownNow();
            tx.executeWithoutResult(status -> {
                em.createQuery("delete from MemberPaymentEntity p where p.id = :id").setParameter("id", ids.get(0)).executeUpdate();
                em.createQuery("delete from MemberChargeEntity c where c.id = :id").setParameter("id", ids.get(1)).executeUpdate();
                em.createQuery("delete from ChargeCycleEntity c where c.id = :id").setParameter("id", ids.get(2)).executeUpdate();
                em.createQuery("delete from ChargeDefinitionEntity d where d.id = :id").setParameter("id", ids.get(3)).executeUpdate();
            });
        }
    }
}
