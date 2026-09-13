package com.jeepclub.backend.billing.infra.storage;

import com.jeepclub.backend.billing.core.application.exception.payment.InvalidPaymentReceiptException;
import com.jeepclub.backend.billing.core.application.service.paymentreceipt.PaymentReceiptValidator;
import com.jeepclub.backend.billing.core.application.service.paymentreceipt.ValidatedPaymentReceipt;
import com.jeepclub.backend.billing.core.port.payment.PaymentReceiptFile;
import com.jeepclub.backend.billing.core.port.payment.PaymentReceiptStoragePort;
import com.jeepclub.backend.billing.core.port.payment.StoredPaymentReceipt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LocalPaymentReceiptStorageAdapter implements PaymentReceiptStoragePort {

    private final PaymentReceiptStorageProperties properties;
    private final PaymentReceiptValidator validator;
    private final Clock clock;

    @Override
    public StoredPaymentReceipt store(PaymentReceiptFile file) {
        ValidatedPaymentReceipt validated = validator.validate(file);
        String storageKey = generateStorageKey(validated.extension());

        Path rootDirectory = properties.rootDirectory()
                .toAbsolutePath()
                .normalize();

        Path targetPath = rootDirectory
                .resolve(storageKey)
                .normalize();

        if (!targetPath.startsWith(rootDirectory)) {
            throw new InvalidPaymentReceiptException("Invalid payment receipt storage path.");
        }

        try {
            Files.createDirectories(targetPath.getParent());
            Files.write(
                    targetPath,
                    validated.content(),
                    StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE
            );
        } catch (IOException exception) {
            throw new InvalidPaymentReceiptException("Could not store payment receipt file.");
        }

        return new StoredPaymentReceipt(
                storageKey,
                buildPublicUrl(storageKey)
        );
    }

    private String generateStorageKey(String extension) {
        LocalDate today = LocalDate.now(clock);

        return "%04d/%02d/%02d/%s.%s".formatted(
                today.getYear(),
                today.getMonthValue(),
                today.getDayOfMonth(),
                UUID.randomUUID(),
                extension
        );
    }

    private String buildPublicUrl(String storageKey) {
        String baseUrl = properties.publicBaseUrl();

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        return baseUrl + "/" + storageKey.replace("\\", "/");
    }
}
