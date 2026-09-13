package com.jeepclub.backend.billing.infra.storage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;

@Component
@Validated
@Setter
@ConfigurationProperties(prefix = "billing.receipts.storage")
public class PaymentReceiptStorageProperties {

    @NotNull
    private Path rootDirectory = Path.of("storage/billing/payment-receipts");

    @NotBlank
    private String publicBaseUrl = "/billing/payment-receipts";

    public Path rootDirectory() {
        return rootDirectory;
    }

    public String publicBaseUrl() {
        return publicBaseUrl;
    }

}
