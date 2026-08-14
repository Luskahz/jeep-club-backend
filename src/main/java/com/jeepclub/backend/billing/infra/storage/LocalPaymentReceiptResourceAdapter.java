package com.jeepclub.backend.billing.infra.storage;

import com.jeepclub.backend.billing.core.application.exception.payment.InvalidPaymentReceiptException;
import com.jeepclub.backend.billing.core.port.payment.PaymentReceiptResourcePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class LocalPaymentReceiptResourceAdapter implements PaymentReceiptResourcePort {

    private final PaymentReceiptStorageProperties properties;

    @Override
    public URI load(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new InvalidPaymentReceiptException("Payment receipt storage key is required.");
        }

        Path rootDirectory = properties.rootDirectory()
                .toAbsolutePath()
                .normalize();

        Path filePath = rootDirectory
                .resolve(storageKey)
                .normalize();

        if (!filePath.startsWith(rootDirectory)) {
            throw new InvalidPaymentReceiptException("Invalid payment receipt path.");
        }

        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new InvalidPaymentReceiptException("Payment receipt file not found.");
        }

        return filePath.toUri();
    }
}
