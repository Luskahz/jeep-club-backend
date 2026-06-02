package com.jeepclub.backend.billing.infra.storage;

import com.jeepclub.backend.billing.core.application.exception.payment.InvalidPaymentReceiptException;
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
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class LocalPaymentReceiptStorageAdapter implements PaymentReceiptStoragePort {

    private final PaymentReceiptStorageProperties properties;
    private final Clock clock;

    @Override
    public StoredPaymentReceipt store(PaymentReceiptFile file) {
        validateFile(file);

        String extension = extractAllowedExtension(file.originalFilename());
        String storageKey = generateStorageKey(extension);

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
                    file.content(),
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

    private void validateFile(PaymentReceiptFile file) {
        Objects.requireNonNull(file, "file cannot be null");

        if (file.content() == null || file.content().length == 0) {
            throw new InvalidPaymentReceiptException("Payment receipt file cannot be empty.");
        }

        if (file.content().length > properties.maxFileSize().toBytes()) {
            throw new InvalidPaymentReceiptException("Payment receipt file exceeds maximum allowed size.");
        }

        if (file.originalFilename() == null || file.originalFilename().isBlank()) {
            throw new InvalidPaymentReceiptException("Payment receipt original filename is required.");
        }

        if (file.contentType() == null || file.contentType().isBlank()) {
            throw new InvalidPaymentReceiptException("Payment receipt content type is required.");
        }

        String normalizedContentType = file.contentType()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (!properties.allowedContentTypes().contains(normalizedContentType)) {
            throw new InvalidPaymentReceiptException("Payment receipt content type is not allowed.");
        }

        extractAllowedExtension(file.originalFilename());
    }

    private String extractAllowedExtension(String originalFilename) {
        String sanitizedFilename = originalFilename.trim();

        int extensionSeparatorIndex = sanitizedFilename.lastIndexOf('.');

        if (extensionSeparatorIndex < 0 || extensionSeparatorIndex == sanitizedFilename.length() - 1) {
            throw new InvalidPaymentReceiptException("Payment receipt file extension is required.");
        }

        String extension = sanitizedFilename
                .substring(extensionSeparatorIndex + 1)
                .toLowerCase(Locale.ROOT);

        if (!properties.allowedExtensions().contains(extension)) {
            throw new InvalidPaymentReceiptException("Payment receipt file extension is not allowed.");
        }

        return extension;
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