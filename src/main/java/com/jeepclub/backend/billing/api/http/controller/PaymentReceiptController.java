package com.jeepclub.backend.billing.api.http.controller;

import com.jeepclub.backend.billing.core.application.exception.payment.InvalidPaymentReceiptException;
import com.jeepclub.backend.billing.core.application.service.paymentreceipt.PaymentReceiptService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class PaymentReceiptController {

    private final PaymentReceiptService paymentReceiptService;

    @GetMapping("/billing/payment-receipts/{year}/{month}/{day}/{filename:.+}")
    public ResponseEntity<Resource> findPaymentReceipt(
            @PathVariable String year,
            @PathVariable String month,
            @PathVariable String day,
            @PathVariable String filename
    ) {
        String storageKey = "%s/%s/%s/%s".formatted(
                year,
                month,
                day,
                filename
        );

        Resource resource = toResource(paymentReceiptService.find(storageKey));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
                .body(resource);
    }

    private Resource toResource(URI resourceUri) {
        try {
            return new UrlResource(resourceUri);
        } catch (MalformedURLException exception) {
            throw new InvalidPaymentReceiptException("Could not read payment receipt file.");
        }
    }
}
