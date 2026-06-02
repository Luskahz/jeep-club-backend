package com.jeepclub.backend.billing.core.domain.model;

import com.jeepclub.backend.billing.core.domain.enums.definition.ChargeDefinitionStatus;
import com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType;
import com.jeepclub.backend.billing.core.domain.enums.cycle.PaymentAcceptancePolicy;
import com.jeepclub.backend.billing.core.domain.exception.definition.ArchivedChargeDefinitionCannotBeActivatedException;
import com.jeepclub.backend.billing.core.domain.exception.definition.ArchivedChargeDefinitionCannotBeDeactivatedException;
import com.jeepclub.backend.billing.core.domain.exception.definition.ArchivedChargeDefinitionCannotBeUpdatedException;
import com.jeepclub.backend.billing.core.domain.exception.definition.ChargeDefinitionAlreadyArchivedException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChargeDefinition {

    private Long id;
    private String name;
    private String description;
    private BigDecimal defaultAmount;
    private ChargeRecurrenceType recurrenceType;
    private Boolean required;
    private PaymentAcceptancePolicy paymentAcceptancePolicy;
    private Integer latePaymentGraceDays;
    private ChargeDefinitionStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant archivedAt;

    private ChargeDefinition(
            Long id,
            String name,
            String description,
            BigDecimal defaultAmount,
            ChargeRecurrenceType recurrenceType,
            Boolean required,
            PaymentAcceptancePolicy paymentAcceptancePolicy,
            Integer latePaymentGraceDays,
            ChargeDefinitionStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant archivedAt
    ) {
        this.id = id;
        this.name = validateName(name);
        this.description = normalizeNullableText(description);
        this.defaultAmount = validateAmount(defaultAmount);
        this.recurrenceType = Objects.requireNonNull(recurrenceType, "recurrenceType cannot be null");
        this.required = Objects.requireNonNull(required, "required cannot be null");
        this.paymentAcceptancePolicy = Objects.requireNonNull(
                paymentAcceptancePolicy,
                "paymentAcceptancePolicy cannot be null"
        );
        this.latePaymentGraceDays = latePaymentGraceDays;
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = updatedAt;
        this.archivedAt = archivedAt;

        validatePaymentAcceptancePolicyConsistency(
                this.paymentAcceptancePolicy,
                this.latePaymentGraceDays
        );

        validateArchiveConsistency();
    }

    public static ChargeDefinition create(
            String name,
            String description,
            BigDecimal defaultAmount,
            ChargeRecurrenceType recurrenceType,
            Boolean required,
            PaymentAcceptancePolicy paymentAcceptancePolicy,
            Integer latePaymentGraceDays,
            Instant now
    ) {
        Objects.requireNonNull(now, "now cannot be null");

        return new ChargeDefinition(
                null,
                name,
                description,
                defaultAmount,
                recurrenceType,
                required,
                paymentAcceptancePolicy,
                latePaymentGraceDays,
                ChargeDefinitionStatus.ACTIVE,
                now,
                null,
                null
        );
    }

    public static ChargeDefinition reconstitute(
            Long id,
            String name,
            String description,
            BigDecimal defaultAmount,
            ChargeRecurrenceType recurrenceType,
            Boolean required,
            PaymentAcceptancePolicy paymentAcceptancePolicy,
            Integer latePaymentGraceDays,
            ChargeDefinitionStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant archivedAt
    ) {
        return new ChargeDefinition(
                id,
                name,
                description,
                defaultAmount,
                recurrenceType,
                required,
                paymentAcceptancePolicy,
                latePaymentGraceDays,
                status,
                createdAt,
                updatedAt,
                archivedAt
        );
    }

    public void update(
            String name,
            String description,
            BigDecimal defaultAmount,
            ChargeRecurrenceType recurrenceType,
            Boolean required,
            PaymentAcceptancePolicy paymentAcceptancePolicy,
            Integer latePaymentGraceDays,
            Instant now
    ) {
        Objects.requireNonNull(now, "now cannot be null");

        if (status == ChargeDefinitionStatus.ARCHIVED) {
            throw new ArchivedChargeDefinitionCannotBeUpdatedException();
        }

        validatePaymentAcceptancePolicyConsistency(
                paymentAcceptancePolicy,
                latePaymentGraceDays
        );

        this.name = validateName(name);
        this.description = normalizeNullableText(description);
        this.defaultAmount = validateAmount(defaultAmount);
        this.recurrenceType = Objects.requireNonNull(recurrenceType, "recurrenceType cannot be null");
        this.required = Objects.requireNonNull(required, "required cannot be null");
        this.paymentAcceptancePolicy = Objects.requireNonNull(
                paymentAcceptancePolicy,
                "paymentAcceptancePolicy cannot be null"
        );
        this.latePaymentGraceDays = latePaymentGraceDays;
        this.updatedAt = now;
    }

    public void deactivate(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        if (status == ChargeDefinitionStatus.ARCHIVED) {
            throw new ArchivedChargeDefinitionCannotBeDeactivatedException();
        }

        this.status = ChargeDefinitionStatus.INACTIVE;
        this.updatedAt = now;
    }

    public void activate(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        if (status == ChargeDefinitionStatus.ARCHIVED) {
            throw new ArchivedChargeDefinitionCannotBeActivatedException();
        }

        this.status = ChargeDefinitionStatus.ACTIVE;
        this.updatedAt = now;
    }

    public void archive(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        if (status == ChargeDefinitionStatus.ARCHIVED) {
            throw new ChargeDefinitionAlreadyArchivedException();
        }

        this.status = ChargeDefinitionStatus.ARCHIVED;
        this.archivedAt = now;
        this.updatedAt = now;
    }

    public boolean isActive() {
        return status == ChargeDefinitionStatus.ACTIVE;
    }

    private static void validatePaymentAcceptancePolicyConsistency(
            PaymentAcceptancePolicy paymentAcceptancePolicy,
            Integer latePaymentGraceDays
    ) {
        Objects.requireNonNull(paymentAcceptancePolicy, "paymentAcceptancePolicy cannot be null");

        if (paymentAcceptancePolicy == PaymentAcceptancePolicy.UNTIL_DAYS_AFTER_DUE_DATE) {
            if (latePaymentGraceDays == null) {
                throw new IllegalArgumentException(
                        "latePaymentGraceDays is required when paymentAcceptancePolicy is UNTIL_DAYS_AFTER_DUE_DATE."
                );
            }

            if (latePaymentGraceDays <= 0) {
                throw new IllegalArgumentException("latePaymentGraceDays must be greater than zero.");
            }

            return;
        }

        if (latePaymentGraceDays != null) {
            throw new IllegalArgumentException(
                    "latePaymentGraceDays must be null when paymentAcceptancePolicy does not use a grace period."
            );
        }
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Charge definition name cannot be blank.");
        }

        return name.trim();
    }

    private static BigDecimal validateAmount(BigDecimal amount) {
        Objects.requireNonNull(amount, "defaultAmount cannot be null");

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("defaultAmount must be greater than zero.");
        }

        return amount;
    }

    private static String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }

    private void validateArchiveConsistency() {
        if (status == ChargeDefinitionStatus.ARCHIVED && archivedAt == null) {
            throw new IllegalArgumentException("archivedAt is required when charge definition is archived.");
        }

        if (status != ChargeDefinitionStatus.ARCHIVED && archivedAt != null) {
            throw new IllegalArgumentException("archivedAt must be null when charge definition is not archived.");
        }
    }
}