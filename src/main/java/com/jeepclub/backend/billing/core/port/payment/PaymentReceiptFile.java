package com.jeepclub.backend.billing.core.port.payment;

public record PaymentReceiptFile(
        String originalFilename,
        String contentType,
        byte[] content
) {
}