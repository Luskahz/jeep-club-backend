package com.jeepclub.backend.billing.infra.storage;

import com.jeepclub.backend.billing.core.application.service.paymentreceipt.PaymentReceiptValidationProperties;
import com.jeepclub.backend.billing.core.application.service.paymentreceipt.PaymentReceiptValidator;
import com.jeepclub.backend.billing.core.port.payment.PaymentReceiptFile;
import com.jeepclub.backend.platform.storage.local.LocalFileStorage;
import com.jeepclub.backend.platform.storage.properties.StorageProperties;
import com.jeepclub.backend.shared.storage.FileStorage;
import com.jeepclub.backend.shared.storage.StorageFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentReceiptLocalStorageIntegrationTest {

    @TempDir Path tempDirectory;

    @Test
    void billingValidationIntegratesWithGlobalLocalProviderInIsolatedDirectory() {
        StorageProperties storageProperties = new StorageProperties();
        storageProperties.setProvider(StorageProperties.Provider.LOCAL);
        storageProperties.local().setRootDirectory(tempDirectory);
        FileStorage storage = new LocalFileStorage(storageProperties, Clock.systemUTC());
        var validated = new PaymentReceiptValidator(new PaymentReceiptValidationProperties())
                .validate(new PaymentReceiptFile("receipt.PNG", "image/png", new byte[]{7, 8, 9}));

        var stored = storage.store(new StorageFile(
                validated.originalFilename(), validated.contentType(), validated.extension(), validated.content()
        ), "billing/payment-receipts");

        assertThat(stored.storageKey()).startsWith("billing/payment-receipts/").endsWith(".png");
        assertThat(storage.load(stored.storageKey()).content()).containsExactly(7, 8, 9);
        assertThat(Path.of(stored.storageKey()).isAbsolute()).isFalse();
        storage.delete(stored.storageKey());
        assertThat(tempDirectory.resolve(stored.storageKey())).doesNotExist();
    }
}
