package com.jeepclub.backend.billing.core.port;

public interface PaymentReceiptStoragePort {

    StoredPaymentReceipt store(PaymentReceiptFile file);
}