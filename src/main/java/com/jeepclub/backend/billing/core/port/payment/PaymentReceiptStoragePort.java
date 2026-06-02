package com.jeepclub.backend.billing.core.port.payment;

public interface PaymentReceiptStoragePort {

    StoredPaymentReceipt store(PaymentReceiptFile file);
}