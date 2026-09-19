package com.jeepclub.backend.billing.core.application.service.paymentreceipt;

import com.jeepclub.backend.billing.core.application.exception.payment.InvalidPaymentReceiptException;
import com.jeepclub.backend.billing.core.port.payment.PaymentReceiptFile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class PaymentReceiptValidator {

    private final PaymentReceiptValidationProperties properties;

    public ValidatedPaymentReceipt validate(PaymentReceiptFile file) {
        if (file == null) {
            throw new InvalidPaymentReceiptException("Payment receipt file is required.");
        }

        byte[] content = file.content();
        if (content == null || content.length == 0) {
            throw new InvalidPaymentReceiptException("Payment receipt file cannot be empty.");
        }

        if (content.length > properties.maxFileSize().toBytes()) {
            throw new InvalidPaymentReceiptException("Payment receipt file exceeds maximum allowed size.");
        }

        String originalFilename = requireText(
                file.originalFilename(),
                "Payment receipt original filename is required."
        );
        String contentType = requireText(
                file.contentType(),
                "Payment receipt content type is required."
        ).toLowerCase(Locale.ROOT);

        if (!properties.allowedContentTypes().contains(contentType)) {
            throw new InvalidPaymentReceiptException("Payment receipt content type is not allowed.");
        }

        String extension = extractExtension(originalFilename);
        if (!properties.allowedExtensions().contains(extension)) {
            throw new InvalidPaymentReceiptException("Payment receipt file extension is not allowed.");
        }

        return new ValidatedPaymentReceipt(originalFilename, contentType, extension, content);
    }

    private static String extractExtension(String originalFilename) {
        int separatorIndex = originalFilename.lastIndexOf('.');
        if (separatorIndex < 0 || separatorIndex == originalFilename.length() - 1) {
            throw new InvalidPaymentReceiptException("Payment receipt file extension is required.");
        }
        return originalFilename.substring(separatorIndex + 1).toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new InvalidPaymentReceiptException(message);
        }
        return value.trim();
    }
}
