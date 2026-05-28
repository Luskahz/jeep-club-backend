package com.jeepclub.backend.billing.core.domain.model;

import com.jeepclub.backend.billing.core.domain.enums.ChargeCycleStatus;
import com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChargeCycle {

    private Long id;
    private Long chargeDefinitionId;
    private String chargeDefinitionNameSnapshot;
    private String chargeDefinitionDescriptionSnapshot;
    private BigDecimal chargeDefinitionDefaultAmountSnapshot;
    private ChargeRecurrenceType chargeDefinitionRecurrenceTypeSnapshot;
    private Boolean chargeDefinitionRequiredSnapshot;
    private String code;
    private LocalDate dueDate;
    private ChargeCycleStatus status;
    private Long generatedByUserId;
    private Instant generatedAt;
    private Instant canceledAt;
    private Long canceledByUserId;
    private Instant createdAt;
    private Instant updatedAt;

    private ChargeCycle(
            Long id,
            Long chargeDefinitionId,
            String chargeDefinitionNameSnapshot,
            String chargeDefinitionDescriptionSnapshot,
            BigDecimal chargeDefinitionDefaultAmountSnapshot,
            ChargeRecurrenceType chargeDefinitionRecurrenceTypeSnapshot,
            Boolean chargeDefinitionRequiredSnapshot,
            String code,
            LocalDate dueDate,
            ChargeCycleStatus status,
            Long generatedByUserId,
            Instant generatedAt,
            Instant canceledAt,
            Long canceledByUserId,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.chargeDefinitionId = validateId(chargeDefinitionId, "chargeDefinitionId");
        this.chargeDefinitionNameSnapshot = validateSnapshotText(
                chargeDefinitionNameSnapshot,
                "chargeDefinitionNameSnapshot"
        );
        this.chargeDefinitionDescriptionSnapshot = normalizeNullableText(chargeDefinitionDescriptionSnapshot);
        this.chargeDefinitionDefaultAmountSnapshot = validateAmount(
                chargeDefinitionDefaultAmountSnapshot,
                "chargeDefinitionDefaultAmountSnapshot"
        );
        this.chargeDefinitionRecurrenceTypeSnapshot = Objects.requireNonNull(
                chargeDefinitionRecurrenceTypeSnapshot,
                "chargeDefinitionRecurrenceTypeSnapshot cannot be null"
        );
        this.chargeDefinitionRequiredSnapshot = Objects.requireNonNull(
                chargeDefinitionRequiredSnapshot,
                "chargeDefinitionRequiredSnapshot cannot be null"
        );
        this.code = validateCode(code);
        this.dueDate = Objects.requireNonNull(dueDate, "dueDate cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.generatedByUserId = generatedByUserId;
        this.generatedAt = generatedAt;
        this.canceledAt = canceledAt;
        this.canceledByUserId = Objects.requireNonNull(canceledByUserId, "canceledByUserId cannot be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = updatedAt;

        validateStatusConsistency();
    }

    public static ChargeCycle generate(
            ChargeDefinition chargeDefinition,
            String code,
            LocalDate dueDate,
            Long generatedByUserId,
            Instant now
    ) {
        Objects.requireNonNull(chargeDefinition, "chargeDefinition cannot be null");
        Objects.requireNonNull(now, "now cannot be null");

        return new ChargeCycle(
                null,
                chargeDefinition.getId(),
                chargeDefinition.getName(),
                chargeDefinition.getDescription(),
                chargeDefinition.getDefaultAmount(),
                chargeDefinition.getRecurrenceType(),
                chargeDefinition.getRequired(),
                code,
                dueDate,
                ChargeCycleStatus.GENERATED,
                generatedByUserId,
                now,
                null,
                null,
                now,
                null
        );
    }

    public static ChargeCycle reconstitute(
            Long id,
            Long chargeDefinitionId,
            String chargeDefinitionNameSnapshot,
            String chargeDefinitionDescriptionSnapshot,
            BigDecimal chargeDefinitionDefaultAmountSnapshot,
            ChargeRecurrenceType chargeDefinitionRecurrenceTypeSnapshot,
            Boolean chargeDefinitionRequiredSnapshot,
            String code,
            LocalDate dueDate,
            ChargeCycleStatus status,
            Long generatedByUserId,
            Instant generatedAt,
            Instant canceledAt,
            Long canceledByUserId,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new ChargeCycle(
                id,
                chargeDefinitionId,
                chargeDefinitionNameSnapshot,
                chargeDefinitionDescriptionSnapshot,
                chargeDefinitionDefaultAmountSnapshot,
                chargeDefinitionRecurrenceTypeSnapshot,
                chargeDefinitionRequiredSnapshot,
                code,
                dueDate,
                status,
                generatedByUserId,
                generatedAt,
                canceledAt,
                canceledByUserId,
                createdAt,
                updatedAt
        );
    }

    public void cancel(Long canceledByUserId, Instant now) {
        validateId(canceledByUserId, "canceledByUserId");
        Objects.requireNonNull(now, "now cannot be null");

        if (status == ChargeCycleStatus.CANCELED) {
            throw new IllegalStateException("Charge cycle is already canceled.");
        }

        this.status = ChargeCycleStatus.CANCELED;
        this.canceledAt = now;
        this.canceledByUserId = canceledByUserId;
        this.updatedAt = now;
    }

    public boolean isGenerated() {
        return status == ChargeCycleStatus.GENERATED;
    }

    public boolean isCanceled() {
        return status == ChargeCycleStatus.CANCELED;
    }

    private void validateStatusConsistency() {
        if (generatedAt == null) {
            throw new IllegalArgumentException("generatedAt is required.");
        }

        if (generatedByUserId != null) {
            validateId(generatedByUserId, "generatedByUserId");
        }

        if (status == ChargeCycleStatus.GENERATED && canceledAt != null) {
            throw new IllegalArgumentException("canceledAt must be null when charge cycle is generated.");
        }

        if (status == ChargeCycleStatus.CANCELED && canceledAt == null) {
            throw new IllegalArgumentException("canceledAt is required when charge cycle is canceled.");
        }
    }

    private static Long validateId(Long id, String fieldName) {
        Objects.requireNonNull(id, fieldName + " cannot be null");

        if (id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero.");
        }

        return id;
    }

    private static String validateCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code cannot be blank.");
        }

        return code.trim();
    }

    private static BigDecimal validateAmount(BigDecimal amount, String fieldName) {
        Objects.requireNonNull(amount, fieldName + " cannot be null");

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero.");
        }

        return amount;
    }

    private static String validateSnapshotText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }

        return value.trim();
    }

    private static String normalizeNullableText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}