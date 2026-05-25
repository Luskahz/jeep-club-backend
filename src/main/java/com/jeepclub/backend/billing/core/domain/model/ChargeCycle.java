package com.jeepclub.backend.billing.core.domain.model;

import com.jeepclub.backend.billing.core.domain.enums.ChargeCycleStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ChargeCycle {

    private Long id;
    private Long chargeDefinitionId;
    private String code;
    private LocalDate dueDate;
    private ChargeCycleStatus status;
    private Long generatedByUserId;
    private Instant generatedAt;
    private Instant canceledAt;
    private Instant createdAt;
    private Instant updatedAt;

    private ChargeCycle(
            Long id,
            Long chargeDefinitionId,
            String code,
            LocalDate dueDate,
            ChargeCycleStatus status,
            Long generatedByUserId,
            Instant generatedAt,
            Instant canceledAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.chargeDefinitionId = validateId(chargeDefinitionId, "chargeDefinitionId");
        this.code = validateCode(code);
        this.dueDate = Objects.requireNonNull(dueDate, "dueDate cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.generatedByUserId = generatedByUserId;
        this.generatedAt = generatedAt;
        this.canceledAt = canceledAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = updatedAt;

        validateStatusConsistency();
    }

    public static ChargeCycle generate(
            Long chargeDefinitionId,
            String code,
            LocalDate dueDate,
            Long generatedByUserId,
            Instant now
    ) {
        Objects.requireNonNull(now, "now cannot be null");

        return new ChargeCycle(
                null,
                chargeDefinitionId,
                code,
                dueDate,
                ChargeCycleStatus.GENERATED,
                generatedByUserId,
                now,
                null,
                now,
                null
        );
    }

    public static ChargeCycle reconstitute(
            Long id,
            Long chargeDefinitionId,
            String code,
            LocalDate dueDate,
            ChargeCycleStatus status,
            Long generatedByUserId,
            Instant generatedAt,
            Instant canceledAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        return new ChargeCycle(
                id,
                chargeDefinitionId,
                code,
                dueDate,
                status,
                generatedByUserId,
                generatedAt,
                canceledAt,
                createdAt,
                updatedAt
        );
    }

    public void cancel(Instant now) {
        Objects.requireNonNull(now, "now cannot be null");

        if (status == ChargeCycleStatus.CANCELED) {
            throw new IllegalStateException("Charge cycle is already canceled.");
        }

        this.status = ChargeCycleStatus.CANCELED;
        this.canceledAt = now;
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
}