package com.jeepclub.backend.billing.core.application.service.paymentreceipt;

import com.jeepclub.backend.shared.storage.FileStorage;
import com.jeepclub.backend.shared.storage.exception.StorageOperationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentReceiptLifecycleTest {

    @Mock FileStorage fileStorage;
    private PaymentReceiptLifecycle lifecycle;

    @BeforeEach
    void setUp() {
        lifecycle = new PaymentReceiptLifecycle(fileStorage);
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void newReceiptRemainsAfterCommit() {
        lifecycle.register("B", null);

        complete(TransactionSynchronization.STATUS_COMMITTED);

        verify(fileStorage, never()).delete(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void newReceiptIsCompensatedAfterRealRollback() {
        lifecycle.register("B", null);

        complete(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(fileStorage).delete("B");
    }

    @Test
    void replacementDeletesOldOnlyAfterCommit() {
        lifecycle.register("B", "A");
        verify(fileStorage, never()).delete(org.mockito.ArgumentMatchers.anyString());

        complete(TransactionSynchronization.STATUS_COMMITTED);

        verify(fileStorage).delete("A");
        verify(fileStorage, never()).delete("B");
    }

    @Test
    void replacementRollbackKeepsOldAndCompensatesNew() {
        lifecycle.register("B", "A");

        complete(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(fileStorage).delete("B");
        verify(fileStorage, never()).delete("A");
    }

    @Test
    void cleanupFailureAfterCommitDoesNotInvalidateNewReceipt() {
        org.mockito.Mockito.doThrow(new StorageOperationException(
                        StorageOperationException.Operation.DELETE,
                        new IllegalStateException("delete failed")
                ))
                .when(fileStorage).delete("A");
        lifecycle.register("B", "A");

        assertThatCode(() -> complete(TransactionSynchronization.STATUS_COMMITTED)).doesNotThrowAnyException();
        verify(fileStorage).delete("A");
        verify(fileStorage, never()).delete("B");
    }

    @Test
    void rollbackCleanupFailureDoesNotReplaceOriginalTransactionOutcome() {
        org.mockito.Mockito.doThrow(new StorageOperationException(
                        StorageOperationException.Operation.DELETE,
                        new IllegalStateException("delete failed")
                ))
                .when(fileStorage).delete("B");
        lifecycle.register("B", null);

        assertThatCode(() -> complete(TransactionSynchronization.STATUS_ROLLED_BACK)).doesNotThrowAnyException();
    }

    @Test
    void neverDeletesKeyThatWasAlreadyCurrent() {
        lifecycle.register("A", "A");

        complete(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(fileStorage, never()).delete(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void missingTransactionCompensatesNewReceiptAndFailsFast() {
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);

        assertThatThrownBy(() -> lifecycle.register("B", null))
                .isInstanceOf(IllegalStateException.class);
        verify(fileStorage).delete("B");
    }

    private static void complete(int status) {
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(status));
    }
}
