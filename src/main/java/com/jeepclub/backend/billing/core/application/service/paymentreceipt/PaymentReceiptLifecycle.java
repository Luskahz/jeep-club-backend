package com.jeepclub.backend.billing.core.application.service.paymentreceipt;

import com.jeepclub.backend.shared.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentReceiptLifecycle {

    private final FileStorage fileStorage;

    public void register(String newStorageKey, String previousStorageKey) {
        Objects.requireNonNull(newStorageKey, "newStorageKey cannot be null");

        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) {
            compensateUncommitted(newStorageKey, previousStorageKey);
            throw new IllegalStateException("Payment receipt lifecycle requires an active transaction.");
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    cleanupPreviousAfterCommit(previousStorageKey, newStorageKey);
                } else {
                    compensateUncommitted(newStorageKey, previousStorageKey);
                }
            }
        });
    }

    private void cleanupPreviousAfterCommit(String previousStorageKey, String currentStorageKey) {
        if (previousStorageKey == null || previousStorageKey.equals(currentStorageKey)) {
            return;
        }
        deleteSafely(previousStorageKey, "Could not remove superseded payment receipt after commit");
    }

    private void compensateUncommitted(String newStorageKey, String previousStorageKey) {
        if (newStorageKey.equals(previousStorageKey)) {
            return;
        }
        deleteSafely(newStorageKey, "Could not compensate uncommitted payment receipt");
    }

    private void deleteSafely(String storageKey, String message) {
        try {
            fileStorage.delete(storageKey);
        } catch (RuntimeException exception) {
            log.error("{}: storageKey={}", message, storageKey, exception);
        }
    }
}
