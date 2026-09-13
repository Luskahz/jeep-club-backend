package com.jeepclub.backend.billing.core.application.service.paymentreceipt;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.util.Set;

@Component
@Validated
@Setter
@ConfigurationProperties(prefix = "billing.receipts.validation")
public class PaymentReceiptValidationProperties {

    @NotNull
    private DataSize maxFileSize = DataSize.ofMegabytes(10);

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
