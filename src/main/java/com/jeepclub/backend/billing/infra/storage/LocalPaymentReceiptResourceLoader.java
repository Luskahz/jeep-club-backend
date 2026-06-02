package com.jeepclub.backend.billing.infra.storage;

import com.jeepclub.backend.billing.core.application.exception.payment.InvalidPaymentReceiptException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@RequiredArgsConstructor
public class LocalPaymentReceiptResourceLoader {

    private final PaymentReceiptStorageProperties properties;

    public Resource load(String storageKey) {
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

        try {
            return new UrlResource(filePath.toUri());
        } catch (MalformedURLException exception) {
            throw new InvalidPaymentReceiptException("Could not read payment receipt file.");
        }
    }
}