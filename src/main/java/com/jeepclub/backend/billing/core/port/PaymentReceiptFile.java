package com.jeepclub.backend.billing.core.port;

public record PaymentReceiptFile(
        String originalFilename,
        String contentType,
        byte[] content
) {
}