package com.jeepclub.backend.billing.core.application.service.paymentreceipt;

import com.jeepclub.backend.shared.storage.FileStorage;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentReceiptLifecycleTransactionIntegrationTest {

    @Mock FileStorage fileStorage;
    private PaymentReceiptLifecycle lifecycle;
    private TransactionTemplate transactions;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:receipt_lifecycle_" + System.nanoTime());
        lifecycle = new PaymentReceiptLifecycle(fileStorage);
        transactions = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    }

    @Test
    void committedReplacementDeletesOldOnlyAfterRealCommit() {
        transactions.executeWithoutResult(status -> {
            lifecycle.register("B", "A");
            verify(fileStorage, never()).delete("A");
            verify(fileStorage, never()).delete("B");
        });

        verify(fileStorage).delete("A");
        verify(fileStorage, never()).delete("B");
    }

    @Test
    void realRollbackCompensatesNewAndPreservesOld() {
        transactions.executeWithoutResult(status -> {
            lifecycle.register("B", "A");
            status.setRollbackOnly();
        });

        verify(fileStorage).delete("B");
        verify(fileStorage, never()).delete("A");
    }

    @Test
    void committedNewReceiptIsNotDeleted() {
        transactions.executeWithoutResult(status -> lifecycle.register("B", null));

        verify(fileStorage, never()).delete("B");
    }
}
