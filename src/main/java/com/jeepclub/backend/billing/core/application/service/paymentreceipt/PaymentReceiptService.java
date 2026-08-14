package com.jeepclub.backend.billing.core.application.service.paymentreceipt;

import com.jeepclub.backend.billing.core.port.payment.PaymentReceiptResourcePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;

@Service
@RequiredArgsConstructor
public class PaymentReceiptService {

    private final PaymentReceiptResourcePort paymentReceiptResourcePort;

    public URI find(String storageKey) {
        return paymentReceiptResourcePort.load(storageKey);
    }
}
