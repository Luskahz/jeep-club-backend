package com.jeepclub.backend.billing.core.application.service.paymentreceipt;

import com.jeepclub.backend.billing.core.application.exception.charge.MemberChargeNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.payment.MemberPaymentAccessDeniedException;
import com.jeepclub.backend.billing.core.application.exception.payment.MemberPaymentNotFoundException;
import com.jeepclub.backend.billing.core.application.exception.payment.PaymentReceiptNotFoundException;
import com.jeepclub.backend.billing.core.application.result.PaymentReceiptResult;
import com.jeepclub.backend.billing.core.domain.model.MemberCharge;
import com.jeepclub.backend.billing.core.domain.model.MemberPayment;
import com.jeepclub.backend.billing.core.repository.MemberChargeRepository;
import com.jeepclub.backend.billing.core.repository.MemberPaymentRepository;
import com.jeepclub.backend.shared.storage.FileStorage;
import com.jeepclub.backend.shared.storage.StorageResource;
import com.jeepclub.backend.shared.storage.exception.StorageObjectNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaymentReceiptService {

    private final MemberPaymentRepository memberPaymentRepository;
    private final MemberChargeRepository memberChargeRepository;
    private final FileStorage fileStorage;

    @Transactional(readOnly = true)
    public PaymentReceiptResult find(Long paymentId, Long authenticatedUserId, boolean hasAdministrativeRead) {
        Objects.requireNonNull(paymentId, "paymentId cannot be null");
        Objects.requireNonNull(authenticatedUserId, "authenticatedUserId cannot be null");

        MemberPayment payment = memberPaymentRepository.findById(paymentId)
                .orElseThrow(() -> new MemberPaymentNotFoundException("Member payment not found."));
        MemberCharge charge = memberChargeRepository.findById(payment.getMemberChargeId())
                .orElseThrow(() -> new MemberChargeNotFoundException("Member charge not found."));

        if (!charge.getUserId().equals(authenticatedUserId) && !hasAdministrativeRead) {
            throw new MemberPaymentAccessDeniedException("Payment receipt does not belong to authenticated user.");
        }

        try {
            StorageResource resource = fileStorage.load(payment.getReceiptStorageKey());
            return new PaymentReceiptResult(resource.content(), contentTypeFor(payment.getReceiptStorageKey()));
        } catch (StorageObjectNotFoundException exception) {
            throw new PaymentReceiptNotFoundException("Payment receipt file not found.");
        }
    }

    private static String contentTypeFor(String storageKey) {
        int separator = storageKey.lastIndexOf('.');
        String extension = separator < 0 ? "" : storageKey.substring(separator + 1).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "pdf" -> "application/pdf";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            default -> throw new PaymentReceiptNotFoundException("Payment receipt file type is invalid.");
        };
    }
}
