package com.jeepclub.backend.memberships;

import com.jeepclub.backend.memberships.core.application.service.membershipapplication.AdminMembershipApplicationService;
import com.jeepclub.backend.memberships.core.domain.model.MembershipApplication;
import com.jeepclub.backend.memberships.core.port.MemberActivationMailSender;
import com.jeepclub.backend.memberships.core.repository.MembershipApplicationRepository;
import com.jeepclub.backend.memberships.infra.persistence.jpa.RejectionNotificationJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import java.time.Clock;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

@SpringBootTest(properties = {"memberships.notifications.worker.enabled=true", "memberships.notifications.poll-delay-ms=100"})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class RejectionNotificationWorkerIntegrationTest {
    @Autowired MembershipApplicationRepository applications;
    @Autowired AdminMembershipApplicationService admin;
    @Autowired RejectionNotificationJpaRepository notifications;
    @Autowired PlatformTransactionManager transactions;
    @Autowired Clock clock;
    @MockitoBean MemberActivationMailSender mail;
    Long pending(String cpf) {
        return new TransactionTemplate(transactions).execute(tx -> applications.saveAndFlush(MembershipApplication.create(
                "Worker candidate", cpf, "worker@example.com", "11999999999", null, clock.instant())).getId());
    }
    @Test void commitWakesTheEnabledWorkerAndSlowSmtpDoesNotBlockTheAdministrativeDecision() throws Exception {
        Long id = pending("00000000811");
        var entered = new CountDownLatch(1); var release = new CountDownLatch(1); var sent = new CountDownLatch(1);
        doAnswer(call -> { entered.countDown(); assertThat(release.await(15, TimeUnit.SECONDS)).isTrue(); sent.countDown(); return null; })
                .when(mail).sendRejectionNotice(any(), any(), nullable(String.class));
        var pool = Executors.newSingleThreadExecutor();
        try {
            pool.submit(() -> admin.reject(id, 7L, null)).get(10, TimeUnit.SECONDS);
            assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(applications.findById(id).orElseThrow().getStatus().name()).isEqualTo("REJECTED");
            assertThat(notifications.findByApplicationId(id).orElseThrow().getState()).isEqualTo("PROCESSING");
            release.countDown(); assertThat(sent.await(10, TimeUnit.SECONDS)).isTrue();
        } finally { release.countDown(); pool.shutdownNow(); }
    }
    @Test void theEnabledWorkerNeverSeesARolledBackRejection() {
        Long id = pending("00000000812");
        new TransactionTemplate(transactions).executeWithoutResult(tx -> { admin.reject(id, 7L, "Original"); tx.setRollbackOnly(); });
        assertThat(notifications.findByApplicationId(id)).isEmpty();
        verify(mail, after(200).never()).sendRejectionNotice(any(), any(), nullable(String.class));
    }
}
