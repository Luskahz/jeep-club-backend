package com.jeepclub.backend.billing.core.domain.model;

import com.jeepclub.backend.billing.core.domain.enums.ChargeDefinitionStatus;
import com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType;
import com.jeepclub.backend.billing.core.domain.exception.definition.ArchivedChargeDefinitionCannotBeUpdatedException;
import com.jeepclub.backend.billing.core.domain.exception.definition.ChargeDefinitionAlreadyArchivedException;
import com.jeepclub.backend.billing.core.domain.exception.definition.ArchivedChargeDefinitionCannotBeActivatedException;
import com.jeepclub.backend.billing.core.domain.exception.definition.ArchivedChargeDefinitionCannotBeDeactivatedException;
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
    private ChargeDefinitionStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    private ChargeDefinition(
            Long id,
            String name,
            String description,
            BigDecimal defaultAmount,
            ChargeRecurrenceType recurrenceType,
            Boolean required,
            ChargeDefinitionStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.name = validateName(name);
        this.description = description;
        this.defaultAmount = validateAmount(defaultAmount);
        this.recurrenceType = Objects.requireNonNull(recurrenceType, "recurrenceType cannot be null");
        this.required = Objects.requireNonNull(required, "required cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = updatedAt;
    }

    public static ChargeDefinition create(
            String name,
            String description,
            BigDecimal defaultAmount,
            ChargeRecurrenceType recurrenceType,
            Boolean required,
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
                ChargeDefinitionStatus.ACTIVE,
                now,
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
            ChargeDefinitionStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new ChargeDefinition(
                id,
                name,
                description,
                defaultAmount,
                recurrenceType,
                required,
                status,
                createdAt,
                updatedAt
        );
    }

    public void update(
            String name,
            String description,
            BigDecimal defaultAmount,
            ChargeRecurrenceType recurrenceType,
            Boolean required,
            Instant now
    ) {
        Objects.requireNonNull(now, "now cannot be null");

        if (status == ChargeDefinitionStatus.ARCHIVED) {
            throw new ArchivedChargeDefinitionCannotBeUpdatedException();
        }

        this.name = validateName(name);
        this.description = description;
        this.defaultAmount = validateAmount(defaultAmount);
        this.recurrenceType = Objects.requireNonNull(recurrenceType, "recurrenceType cannot be null");
        this.required = Objects.requireNonNull(required, "required cannot be null");
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
        this.updatedAt = now;
    }

    public boolean isActive() {
        return status == ChargeDefinitionStatus.ACTIVE;
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
}