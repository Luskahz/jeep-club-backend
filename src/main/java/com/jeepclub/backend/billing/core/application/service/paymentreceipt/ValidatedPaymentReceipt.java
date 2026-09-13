package com.jeepclub.backend.billing.core.application.service.paymentreceipt;

public record ValidatedPaymentReceipt(
        String originalFilename,
        String contentType,
        String extension,
        byte[] content
) {
    public ValidatedPaymentReceipt {
        content = content == null ? null : content.clone();
    }

    @Override
    public byte[] content() {
        return content == null ? null : content.clone();
    }
}
