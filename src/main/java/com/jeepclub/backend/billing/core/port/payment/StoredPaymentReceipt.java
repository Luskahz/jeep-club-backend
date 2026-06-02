package com.jeepclub.backend.billing.core.port.payment;

public record StoredPaymentReceipt(
        String storageKey,
        String url
) {
}