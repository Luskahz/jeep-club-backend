package com.jeepclub.backend.billing.core.application.service.paymentreceipt;

import com.jeepclub.backend.billing.core.application.exception.payment.InvalidPaymentReceiptException;
import com.jeepclub.backend.billing.core.port.payment.PaymentReceiptFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentReceiptValidatorTest {

    private PaymentReceiptValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PaymentReceiptValidator(new PaymentReceiptValidationProperties());
    }

    @Test
    void validatesAReceiptWithoutFilesystemAndNormalizesExtensionAndMime() {
        var result = validator.validate(new PaymentReceiptFile(" receipt.PDF ", " APPLICATION/PDF ", new byte[]{1}));

        assertThat(result.originalFilename()).isEqualTo("receipt.PDF");
        assertThat(result.contentType()).isEqualTo("application/pdf");
        assertThat(result.extension()).isEqualTo("pdf");
    }

    @Test
    void rejectsMissingAndEmptyFiles() {
        assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(InvalidPaymentReceiptException.class)
                .hasMessage("Payment receipt file is required.");
        assertThatThrownBy(() -> validator.validate(new PaymentReceiptFile("receipt.pdf", "application/pdf", new byte[0])))
                .isInstanceOf(InvalidPaymentReceiptException.class)
                .hasMessage("Payment receipt file cannot be empty.");
    }

    @Test
    void preservesTenMegabyteDefaultAndRejectsOnlyLargerFiles() {
        PaymentReceiptValidationProperties properties = new PaymentReceiptValidationProperties();
        assertThat(properties.maxFileSize()).isEqualTo(DataSize.ofMegabytes(10));

        assertThat(validator.validate(new PaymentReceiptFile(
                "receipt.pdf", "application/pdf", new byte[(int) DataSize.ofMegabytes(10).toBytes()]
        )).extension()).isEqualTo("pdf");

        assertThatThrownBy(() -> validator.validate(new PaymentReceiptFile(
                "receipt.pdf", "application/pdf", new byte[(int) DataSize.ofMegabytes(10).toBytes() + 1]
        ))).isInstanceOf(InvalidPaymentReceiptException.class)
                .hasMessage("Payment receipt file exceeds maximum allowed size.");
    }

    @Test
    void rejectsInvalidMimeAndExtension() {
        assertThatThrownBy(() -> validator.validate(new PaymentReceiptFile("receipt.pdf", "text/plain", new byte[]{1})))
                .isInstanceOf(InvalidPaymentReceiptException.class)
                .hasMessage("Payment receipt content type is not allowed.");
        assertThatThrownBy(() -> validator.validate(new PaymentReceiptFile("receipt.exe", "application/pdf", new byte[]{1})))
                .isInstanceOf(InvalidPaymentReceiptException.class)
                .hasMessage("Payment receipt file extension is not allowed.");
    }

    @Test
    void normalizesEveryAllowedUppercaseExtension() {
        assertThat(validator.validate(file("a.PDF", "application/pdf")).extension()).isEqualTo("pdf");
        assertThat(validator.validate(file("a.JPG", "image/jpeg")).extension()).isEqualTo("jpg");
        assertThat(validator.validate(file("a.JPEG", "image/jpeg")).extension()).isEqualTo("jpeg");
        assertThat(validator.validate(file("a.PNG", "image/png")).extension()).isEqualTo("png");
        assertThat(validator.validate(file("a.WEBP", "image/webp")).extension()).isEqualTo("webp");
    }

    private static PaymentReceiptFile file(String name, String mime) {
        return new PaymentReceiptFile(name, mime, new byte[]{1});
    }
}
