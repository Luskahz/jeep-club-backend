package com.jeepclub.backend.billing.infra.storage;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.nio.file.Path;
import java.util.Set;

@Component
@Validated
@Setter
@ConfigurationProperties(prefix = "billing.receipts.storage")
public class PaymentReceiptStorageProperties {

    @NotNull
    private Path rootDirectory = Path.of("storage/billing/payment-receipts");

    @NotBlank
    private String publicBaseUrl = "/billing/payment-receipts";

    @NotNull
    private DataSize maxFileSize = DataSize.ofMegabytes(5);

    @NotEmpty
    private Set<String> allowedContentTypes = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    @NotEmpty
    private Set<String> allowedExtensions = Set.of(
            "pdf",
            "jpg",
            "jpeg",
            "png",
            "webp"
    );

    public Path rootDirectory() {
        return rootDirectory;
    }

    public String publicBaseUrl() {
        return publicBaseUrl;
    }

    public DataSize maxFileSize() {
        return maxFileSize;
    }

    public Set<String> allowedContentTypes() {
        return allowedContentTypes;
    }

    public Set<String> allowedExtensions() {
        return allowedExtensions;
    }
}