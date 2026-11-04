package com.jeepclub.backend.platform.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jeepclub.backend.billing.core.application.service.paymentreceipt.PaymentReceiptLifecycle;
import com.jeepclub.backend.platform.web.exception.GlobalExceptionHandler;
import com.jeepclub.backend.shared.storage.FileStorage;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperationalLogPrivacyTest {
    private static final String PRIVATE_VALUE = "cpf=41876293040 email=synthetic@example.com token=secret-token";
    private static final String STORAGE_KEY = "billing/payment-receipts/private-receipt.pdf";

    @Test
    void unexpectedHttpFailureKeepsGenericResponseAndSafeDiagnostic() {
        assertSafeDiagnostic(GlobalExceptionHandler.class, () -> {
            var problem = new GlobalExceptionHandler().handleUnexpectedException(
                    failure(), Locale.forLanguageTag("pt-BR"));
            assertThat(problem.getStatus()).isEqualTo(500);
            assertThat(problem.getDetail()).doesNotContain(PRIVATE_VALUE);
        });
    }

    @Test
    void failedAuditPersistenceDoesNotExposeDatabaseMessagesOrNestedCauses() {
        var queue = mock(SystemLogServiceAdapter.class);
        var repository = mock(SystemLogJpaRepository.class);
        when(queue.drain(100)).thenReturn(List.of(event()));
        when(repository.saveAll(any())).thenThrow(failure());
        var worker = new SystemLogPersistenceWorker(queue, repository);

        assertSafeDiagnostic(SystemLogPersistenceWorker.class, worker::persistNextBatch);
        doThrow(failure()).when(repository).deleteOlderThan(any());
        assertSafeDiagnostic(SystemLogPersistenceWorker.class, worker::deleteExpiredLogs);
    }

    @Test
    void receiptCompensationFailureDoesNotExposeStorageKeyOrCause() {
        var storage = mock(FileStorage.class);
        doThrow(failure()).when(storage).delete(STORAGE_KEY);
        var lifecycle = new PaymentReceiptLifecycle(storage);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
        try {
            lifecycle.register(STORAGE_KEY, null);
            assertSafeDiagnostic(PaymentReceiptLifecycle.class, () ->
                    TransactionSynchronizationManager.getSynchronizations().forEach(
                            sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void queuedAuditKeepsRequiredTargetWithoutCopyingItToOperationalLog() {
        var queue = new SystemLogServiceAdapter(mock(SystemLogJpaRepository.class));
        var logger = (Logger) LoggerFactory.getLogger(SystemLogServiceAdapter.class);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            queue.record(event());
            assertThat(appender.list).hasSize(1);
            assertThat(appender.list.get(0).getFormattedMessage())
                    .contains("action=CLINICAL_READ", "actorId=7")
                    .doesNotContain("path=", PRIVATE_VALUE);
            assertThat(queue.drain(100)).containsExactly(event());
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    private static DataIntegrityViolationException failure() {
        return new DataIntegrityViolationException(PRIVATE_VALUE,
                new IllegalArgumentException(STORAGE_KEY + " " + PRIVATE_VALUE));
    }

    private static SystemLogEvent event() {
        return new SystemLogEvent(7L, "CLINICAL_READ", "GET", "/health/" + PRIVATE_VALUE,
                200, SystemLogOutcome.SUCCESS, 0, "synthetic-request", Instant.EPOCH);
    }

    private static void assertSafeDiagnostic(Class<?> source, Runnable operation) {
        var logger = (Logger) LoggerFactory.getLogger(source);
        var appender = new ListAppender<ILoggingEvent>();
        appender.start();
        logger.addAppender(appender);
        try {
            operation.run();
            assertThat(appender.list).hasSize(1);
            var event = appender.list.get(0);
            assertThat(event.getFormattedMessage()).contains("category=DataIntegrityViolationException")
                    .doesNotContain(PRIVATE_VALUE, STORAGE_KEY, "41876293040", "secret-token");
            assertThat(event.getThrowableProxy()).isNull();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }
}
