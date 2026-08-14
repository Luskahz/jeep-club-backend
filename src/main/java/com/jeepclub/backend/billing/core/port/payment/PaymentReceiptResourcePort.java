package com.jeepclub.backend.billing.core.port.payment;

import java.net.URI;

public interface PaymentReceiptResourcePort {

    URI load(String storageKey);
}
