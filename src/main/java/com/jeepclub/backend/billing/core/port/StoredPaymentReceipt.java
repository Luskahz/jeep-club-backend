package com.jeepclub.backend.billing.core.port;

public record StoredPaymentReceipt(
        String storageKey,
        String url
) {
}