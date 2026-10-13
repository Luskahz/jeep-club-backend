package com.jeepclub.backend.platform.logging;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SystemLogTransactionalAuditIntegrationTest {
    @Autowired private SystemLogService audit;
    @Autowired private SystemLogJpaRepository logs;
    @Autowired private TransactionTemplate transactions;

    @Test
    void requiredBusinessAuditRollsBackWithItsOperation() {
        SystemLogEvent event = new SystemLogEvent(17L, "BACK_511_AUDIT_ROLLBACK_TEST", "POST",
                "/identity/admin/users/42", 201, SystemLogOutcome.SUCCESS, 0, null, Instant.now());
        transactions.executeWithoutResult(status -> {
            audit.recordInTransaction(event);
            assertThat(logs.findAll()).anyMatch(log -> event.action().equals(log.getAction()));
            status.setRollbackOnly();
        });
        assertThat(logs.findAll()).noneMatch(log -> event.action().equals(log.getAction()));
    }
}
