package com.jeepclub.backend.billing.core.application.result;

public record PaymentReceiptResult(byte[] content, String contentType) {
    public PaymentReceiptResult {
        content = content == null ? null : content.clone();
    }

    @Override
    public byte[] content() {
        return content == null ? null : content.clone();
    }

    public long size() {
        return content == null ? 0 : content.length;
    }
}
