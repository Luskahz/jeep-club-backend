package com.jeepclub.backend.billing.infra.persistence.adapter;

import com.jeepclub.backend.billing.core.application.service.memberrefund.AdminMemberRefundService;
import com.jeepclub.backend.billing.core.application.service.memberrefund.MemberRefundService;
import com.jeepclub.backend.billing.core.application.result.MemberRefundResult;
import com.jeepclub.backend.billing.core.domain.enums.payment.PaymentMethod;
import com.jeepclub.backend.billing.core.domain.enums.refund.MemberRefundStatus;
import com.jeepclub.backend.billing.core.domain.exception.refund.InvalidMemberRefundStateException;
import com.jeepclub.backend.billing.core.domain.model.MemberPayment;
import com.jeepclub.backend.billing.core.domain.model.MemberRefund;
import com.jeepclub.backend.billing.core.repository.MemberChargeRepository;
import com.jeepclub.backend.billing.core.repository.MemberPaymentRepository;
import com.jeepclub.backend.billing.core.repository.MemberRefundRepository;
import com.jeepclub.backend.billing.infra.persistence.mapper.MemberChargeMapper;
import com.jeepclub.backend.billing.infra.persistence.mapper.MemberPaymentMapper;
import com.jeepclub.backend.billing.infra.persistence.mapper.MemberRefundMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.jeepclub.backend.billing.support.BillingFixtures.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({MemberRefundAdapter.class, MemberRefundMapper.class, MemberPaymentAdapter.class, MemberPaymentMapper.class,
        MemberChargeAdapter.class, MemberChargeMapper.class, AdminMemberRefundService.class, MemberRefundService.class,
        BillingRefundConcurrencyIntegrationTest.TimeConfiguration.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BillingRefundConcurrencyIntegrationTest {
    @Autowired MemberRefundRepository refunds;
    @Autowired MemberPaymentRepository payments;
    @Autowired MemberChargeRepository charges;
    @Autowired AdminMemberRefundService admin;
    @Autowired MemberRefundService member;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired EntityManager em;
    @MockitoSpyBean MemberRefundAdapter refundAdapter;

    @TestConfiguration
    static class TimeConfiguration {
        @Bean Clock billingClock() { return CLOCK; }
    }

    enum Race {
        APPROVE_REJECT, REJECT_APPROVE, APPROVE_CANCEL, CANCEL_APPROVE,
        REFUND_CANCEL, CANCEL_REFUND, REQUEST_CANCEL, CANCEL_REQUEST,
        REQUEST_APPROVE, APPROVE_REQUEST, EXPIRE_CANCEL, CANCEL_EXPIRE,
        PAYMENT_REQUEST_CANCEL, CANCEL_PAYMENT_REQUEST, PAYMENT_REQUEST_REQUEST,
        REFUND_PAYMENT_REQUEST
    }

    record Seed(Long refundId, Long paymentId, Long chargeId, MemberPayment payment) {}

    @Test
    void cancellationAfterCommittedApprovalRemainsValidAndRetainsApprovalHistory() {
        var tx = new TransactionTemplate(transactionManager);
        Long id = tx.execute(status -> refunds.save(eligibility()).getId());
        try {
            admin.approve(id, 99L);
            admin.cancel(id, 98L);
            tx.executeWithoutResult(status -> {
                var restored = refunds.findById(id).orElseThrow();
                assertThat(restored.isCanceled()).isTrue();
                assertThat(restored.getApprovedAt()).isEqualTo(NOW);
                assertThat(restored.getApprovedByUserId()).isEqualTo(99L);
                assertThat(restored.getCanceledAt()).isEqualTo(NOW);
                assertThat(restored.getCanceledByUserId()).isEqualTo(98L);
            });
        } finally {
            tx.executeWithoutResult(status -> em.createQuery("delete from MemberRefundEntity r where r.id = :id")
                    .setParameter("id", id).executeUpdate());
        }
    }

    @ParameterizedTest
    @EnumSource(Race.class)
    void overlappingCommandsHaveOneCommitAndOneControlledConflict(Race race) throws Exception {
        var tx = new TransactionTemplate(transactionManager);
        Seed seed = tx.execute(status -> {
            var debt = charges.save(com.jeepclub.backend.billing.core.domain.model.MemberCharge.create(
                    10L, 3L, 4L, AMOUNT, DUE,
                    com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy.AFTER_DUE_DATE, null, NOW));
            debt.markAsPaid(NOW, NOW);
            charges.save(debt);
            var payment = MemberPayment.submitForValidation(debt.getId(), AMOUNT, PaymentMethod.PIX, NOW, "receipt", null, NOW);
            payment.confirm(99L, NOW);
            payment = payments.save(payment);
            var refund = MemberRefund.createEligibilityForCanceledCycle(debt.getId(), payment.getId(), 4L, 10L,
                    AMOUNT, 99L, NOW.minus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(29)), NOW);
            if (race == Race.APPROVE_REJECT || race == Race.REJECT_APPROVE
                    || race == Race.APPROVE_CANCEL || race == Race.CANCEL_APPROVE) refund.request(10L, NOW);
            if (race == Race.REFUND_CANCEL || race == Race.CANCEL_REFUND || race == Race.REFUND_PAYMENT_REQUEST) refund.approve(99L, NOW);
            if (race == Race.EXPIRE_CANCEL || race == Race.CANCEL_EXPIRE) {
                refund = MemberRefund.createEligibilityForCanceledCycle(debt.getId(), payment.getId(), 4L, 10L,
                        AMOUNT, 99L, NOW.minus(Duration.ofDays(40)), NOW.minus(Duration.ofDays(10)), NOW.minus(Duration.ofDays(40)));
            }
            return new Seed(refunds.save(refund).getId(), payment.getId(), debt.getId(), payment);
        });
        var savedBeforeCommit = new CountDownLatch(1);
        var allowCommit = new CountDownLatch(1);
        var firstSave = new AtomicBoolean(true);
        var expected = new AtomicReference<MemberRefund>();
        // Hold the winning service inside its real transaction, after its locked read and save.
        // Without the lock the second command can save and succeed, so this detects lost-update regressions.
        doAnswer(invocation -> {
            MemberRefund saved = (MemberRefund) invocation.callRealMethod();
            if (saved.getId().equals(seed.refundId()) && firstSave.compareAndSet(true, false)) {
                expected.set(new MemberRefundMapper().toDomain(new MemberRefundMapper().toEntity(saved)));
                savedBeforeCommit.countDown();
                if (!allowCommit.await(15, TimeUnit.SECONDS)) throw new AssertionError("commit barrier timed out");
            }
            return saved;
        }).when(refundAdapter).save(any());

        var executor = Executors.newFixedThreadPool(2);
        try {
            var winner = executor.submit(() -> command(race, true, seed));
            assertThat(savedBeforeCommit.await(15, TimeUnit.SECONDS)).as("winner reached save under lock").isTrue();
            var loser = executor.submit(() -> catchThrowable(() -> command(race, false, seed)));
            var conflict = loser.get(10, TimeUnit.SECONDS);
            assertThat(conflict).isInstanceOf(InvalidMemberRefundStateException.class);
            var response = new com.jeepclub.backend.billing.api.http.exception.BillingMemberRefundExceptionHandler()
                    .handleInvalidMemberRefundState((InvalidMemberRefundStateException) conflict);
            assertThat(response.getStatusCode().value()).isEqualTo(409);
            assertThat(response.getBody().getCode()).isEqualTo("INVALID_MEMBER_REFUND_STATE");
            allowCommit.countDown();
            assertThat(winner.get(15, TimeUnit.SECONDS).status()).isEqualTo(expected.get().getStatus());
            tx.executeWithoutResult(status -> {
                var finalRefund = refunds.findById(seed.refundId()).orElseThrow();
                assertThat(finalRefund).usingRecursiveComparison().isEqualTo(expected.get());
                assertThat(finalRefund.getAmount()).isEqualByComparingTo(AMOUNT);
                assertThat(payments.findById(seed.paymentId()).orElseThrow()).usingRecursiveComparison().isEqualTo(seed.payment());
                assertThat(charges.findById(seed.chargeId()).orElseThrow().isPaid()).isTrue();
                assertThat(em.createQuery("select count(r) from MemberRefundEntity r where r.memberPaymentId = :payment", Long.class)
                        .setParameter("payment", seed.paymentId()).getSingleResult()).isEqualTo(1L);
                assertThat(em.createQuery("select count(r) from MemberRefundEntity r where r.memberPaymentId = :payment and r.status = :state", Long.class)
                        .setParameter("payment", seed.paymentId()).setParameter("state", MemberRefundStatus.REFUNDED).getSingleResult())
                        .isEqualTo(finalRefund.isRefunded() ? 1L : 0L);
            });
        } finally {
            allowCommit.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
            tx.executeWithoutResult(status -> {
                em.createQuery("delete from MemberRefundEntity r where r.id = :id").setParameter("id", seed.refundId()).executeUpdate();
                em.createQuery("delete from MemberPaymentEntity p where p.id = :id").setParameter("id", seed.paymentId()).executeUpdate();
                em.createQuery("delete from MemberChargeEntity c where c.id = :id").setParameter("id", seed.chargeId()).executeUpdate();
            });
        }
    }

    private MemberRefundResult command(Race race, boolean first, Seed seed) {
        String operation = switch (race) {
            case APPROVE_REJECT -> first ? "approve" : "reject";
            case REJECT_APPROVE -> first ? "reject" : "approve";
            case APPROVE_CANCEL -> first ? "approve" : "cancel";
            case CANCEL_APPROVE -> first ? "cancel" : "approve";
            case REFUND_CANCEL -> first ? "refund" : "cancel";
            case CANCEL_REFUND -> first ? "cancel" : "refund";
            case REQUEST_CANCEL -> first ? "request" : "cancel";
            case CANCEL_REQUEST -> first ? "cancel" : "request";
            case REQUEST_APPROVE -> first ? "request" : "approve";
            case APPROVE_REQUEST -> first ? "approve" : "request";
            case EXPIRE_CANCEL -> first ? "expire" : "cancel";
            case CANCEL_EXPIRE -> first ? "cancel" : "expire";
            case PAYMENT_REQUEST_CANCEL -> first ? "paymentRequest" : "cancel";
            case CANCEL_PAYMENT_REQUEST -> first ? "cancel" : "paymentRequest";
            case PAYMENT_REQUEST_REQUEST -> first ? "paymentRequest" : "request";
            case REFUND_PAYMENT_REQUEST -> first ? "refund" : "paymentRequest";
        };
        return switch (operation) {
            case "approve" -> admin.approve(seed.refundId(), 99L);
            case "reject" -> admin.reject(seed.refundId(), 98L, "review rejected");
            case "cancel" -> admin.cancel(seed.refundId(), 97L);
            case "refund" -> admin.markAsRefunded(seed.refundId(), 96L);
            case "expire" -> admin.expire(seed.refundId());
            case "request" -> member.request(seed.refundId(), 10L);
            case "paymentRequest" -> member.requestByMemberPaymentId(10L, seed.paymentId());
            default -> throw new AssertionError(operation);
        };
    }
}
