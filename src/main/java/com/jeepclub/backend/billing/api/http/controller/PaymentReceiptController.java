package com.jeepclub.backend.billing.api.http.controller;

import com.jeepclub.backend.billing.infra.storage.LocalPaymentReceiptResourceLoader;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequiredArgsConstructor
public class PaymentReceiptController {

    private final LocalPaymentReceiptResourceLoader resourceLoader;

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

        Resource resource = resourceLoader.load(storageKey);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate())
                .body(resource);
    }
}