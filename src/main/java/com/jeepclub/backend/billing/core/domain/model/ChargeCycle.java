package com.jeepclub.backend.billing.core.domain.model;

import com.jeepclub.backend.billing.core.domain.enums.ChargeCycleStatus;
import com.jeepclub.backend.billing.core.domain.enums.ChargeRecurrenceType;
import com.jeepclub.backend.billing.core.domain.enums.PaymentAcceptancePolicy;
import com.jeepclub.backend.billing.core.domain.exception.cycle.ChargeCycleAlreadyCanceledException;
import com.jeepclub.backend.billing.core.domain.exception.cycle.ChargeCycleCannotBeArchivedException;
import com.jeepclub.backend.billing.core.domain.exception.cycle.ChargeCycleCannotBeCanceledException;
import com.jeepclub.backend.billing.core.domain.exception.cycle.ChargeCycleCannotBeFinishedException;
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
    private PaymentAcceptancePolicy chargeDefinitionPaymentAcceptancePolicySnapshot;
    private Integer chargeDefinitionLatePaymentGraceDaysSnapshot;
    private String code;
    private LocalDate dueDate;
    private ChargeCycleStatus status;
    private Long generatedByUserId;
    private Instant generatedAt;
    private Instant canceledAt;
    private Long canceledByUserId;
    private Instant finishedAt;
    private Long finishedByUserId;
    private Instant archivedAt;
    private Long archivedByUserId;
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
            PaymentAcceptancePolicy chargeDefinitionPaymentAcceptancePolicySnapshot,
            Integer chargeDefinitionLatePaymentGraceDaysSnapshot,
            String code,
            LocalDate dueDate,
            ChargeCycleStatus status,
            Long generatedByUserId,
            Instant generatedAt,
            Instant canceledAt,
            Long canceledByUserId,
            Instant finishedAt,
            Long finishedByUserId,
            Instant archivedAt,
            Long archivedByUserId,
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
                chargeDefinitionRecurrenceDefinitionDescriptionSnapshot);
        this.chargeDefinitionDefaultAmountSnapshot = validateAmount(
                chargeDefinitionDefaultAmountSnapshot,
                "chargeDefinitionDefaultAmountSnapshot"
        );
        this.chargeDefinitionRecTypeSnapshot,
                "chargeDefinitionRecurrenceTypeSnapshot cannot be null"
        );
        this.chargeDefinitionRequiredSnapshot = Objects.requireNonNull(
                chargeDefinitionRequiredSnapshot,
                "chargeDefinitionRequiredSnapshot cannot be null"
        );
        this.chargeDefinitionPaymentAcceptancePolicySnapshot = Objects.requireNonNull(
                chargeDefinitionPaymentAcceptancePolicySnapshot,
                "chargeDefinitionPaymentAcceptancePolicySnapshot cannot be null"
        );
        this.chargeDefinitionLatePaymentGraceDaysSnapshot = chargeDefinitionLatePaymentGraceDaysSnapshot;
        this.code = validateCode(code);
        this.dueDate = Objects.requireNonNull(dueDate, "dueDate cannot be null");
        this.status = Objects.requireNonNull(status, "status cannot be null");
        this.generatedByUserId = generatedByUserId;
        this.generatedAt = generatedAt;
        this.canceledAt = canceledAt;
        this.canceledByUserId = canceledByUserId;
        this.finishedAt = finishedAt;
        this.finishedByUserId = finishedByUserId;
        this.archivedAt = archivedAt;
        this.archivedByUserId = archivedByUserId;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt cannot be null");
        this.updatedAt = updatedAt;

        validatePaymentAcceptancePolicyConsistency(
                this.chargeDefinitionPaymentAcceptancePolicySnapshot,
                this.chargeDefinitionLatePaymentGraceDaysSnapshot
        );

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
                chargeDefinition.getPaymentAcceptancePolicy(),
                chargeDefinition.getLatePaymentGraceDays(),
                code,
                dueDate,
                ChargeCycleStatus.GENERATED,
                generatedByUserId,
                now,
                null,
                null,
                null,
                null,
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
            PaymentAcceptancePolicy chargeDefinitionPaymentAcceptancePolicySnapshot,
            Integer chargeDefinitionLatePaymentGraceDaysSnapshot,
            String code,
            LocalDate dueDate,
            ChargeCycleStatus status,
            Long generatedByUserId,
            Instant generatedAt,
            Instant canceledAt,
            Long canceledByUserId,
            Instant finishedAt,
            Long finishedByUserId,
            Instant archivedAt,
            Long archivedByUserId,
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
                chargeDefinitionPaymentAcceptancePolicySnapshot,
                chargeDefinitionLatePaymentGraceDaysSnapshot,
                code,
                dueDate,
                status,
                generatedByUserId,
                generatedAt,
                canceledAt,
                canceledByUserId,
                finishedAt,
                finishedByUserId,
                archivedAt,
                archivedByUserId,
                createdAt,
                updatedAt
        );
    }

    public void cancel(Long canceledByUserId, Instant now) {
        validateId(canceledByUserId, "canceledByUserId");
        Objects.requireNonNull(now, "now cannot be null");

        if (status == ChargeCycleStatus.CANCELED) {
            throw new ChargeCycleAlreadyCanceledException();
        }

        if (status != ChargeCycleStatus.GENERATED) {
            throw new ChargeCycleCannotBeCanceledException(
                    "Only generated charge cycles can be canceled."
            );
        }

        this.status = ChargeCycleStatus.CANCELED;
        this.canceledAt = now;
        this.canceledByUserId = canceledByUserId;
        this.updatedAt = now;
    }

    public void finish(Long finishedByUserId, Instant now) {
        validateId(finishedByUserId, "finishedByUserId");
        Objects.requireNonNull(now, "now cannot be null");

        if (status != ChargeCycleStatus.GENERATED) {
            throw new ChargeCycleCannotBeFinishedException(
                    "Only generated charge cycles can be finished."
            );
        }

        this.status = ChargeCycleStatus.FINISHED;
        this.finishedAt = now;
        this.finishedByUserId = finishedByUserId;
        this.updatedAt = now;
    }

    public void archive(Long archivedByUserId, Instant now) {
        validateId(archivedByUserId, "archivedByUserId");
        Objects.requireNonNull(now, "now cannot be null");

        if (status != ChargeCycleStatus.FINISHED && status != ChargeCycleStatus.CANCELED) {
            throw new ChargeCycleCannotBeArchivedException(
                    "Only finished or canceled charge cycles can be archived."
            );
        }

        this.status = ChargeCycleStatus.ARCHIVED;
        this.archivedAt = now;
        this.archivedByUserId = archivedByUserId;
        this.updatedAt = now;
    }

    public boolean isGenerated() {
        return status == ChargeCycleStatus.GENERATED;
    }

    public boolean isCanceled() {
        return status == ChargeCycleStatus.CANCELED;
    }

    public boolean isFinished() {
        return status == ChargeCycleStatus.FINISHED;
    }

    public boolean isArchived() {
        return status == ChargeCycleStatus.ARCHIVED;
    }

    private void validateStatusConsistency() {
        if (generatedAt == null) {
            throw new IllegalArgumentException("generatedAt is required.");
        }

        if (generatedByUserId != null) {
            validateId(generatedByUserId, "generatedByUserId");
        }

        validateOptionalId(canceledByUserId, "canceledByUserId");
        validateOptionalId(finishedByUserId, "finishedByUserId");
        validateOptionalId(archivedByUserId, "archivedByUserId");

        if (status == ChargeCycleStatus.GENERATED) {
            ensureNoCancellationData();
            ensureNoFinishData();
            ensureNoArchiveData();
        }

        if (status == ChargeCycleStatus.CANCELED) {
            requireCancellationData();
            ensureNoFinishData();
            ensureNoArchiveData();
        }

        if (status == ChargeCycleStatus.FINISHED) {
            requireFinishData();
            ensureNoCancellationData();
            ensureNoArchiveData();
        }

        if (status == ChargeCycleStatus.ARCHIVED) {
            requireArchiveData();
            ensureArchivedSourceIsValid();
        }
    }

    private void requireCancellationData() {
        if (canceledAt == null) {
            throw new IllegalArgumentException("canceledAt is required when charge cycle is canceled.");
        }

        validateId(canceledByUserId, "canceledByUserId");
    }

    private void requireFinishData() {
        if (finishedAt == null) {
            throw new IllegalArgumentException("finishedAt is required when charge cycle is finished.");
        }

        validateId(finishedByUserId, "finishedByUserId");
    }

    private void requireArchiveData() {
        if (archivedAt == null) {
            throw new IllegalArgumentException("archivedAt is required when charge cycle is archived.");
        }

        validateId(archivedByUserId, "archivedByUserId");
    }

    private void ensureArchivedSourceIsValid() {
        boolean archivedCanceledCycle = hasCancellationData() && !hasFinishData();
        boolean archivedFinishedCycle = hasFinishData() && !hasCancellationData();

        if (!archivedCanceledCycle && !archivedFinishedCycle) {
            throw new IllegalArgumentException(
                    "Archived charge cycle must preserve either cancellation data or finish data."
            );
        }
    }

    private void ensureNoCancellationData() {
        if (canceledAt != null) {
            throw new IllegalArgumentException("canceledAt must be null in current charge cycle status.");
        }

        if (canceledByUserId != null) {
            throw new IllegalArgumentException("canceledByUserId must be null in current charge cycle status.");
        }
    }

    private void ensureNoFinishData() {
        if (finishedAt != null) {
            throw new IllegalArgumentException("finishedAt must be null in current charge cycle status.");
        }

        if (finishedByUserId != null) {
            throw new IllegalArgumentException("finishedByUserId must be null in current charge cycle status.");
        }
    }

    private void ensureNoArchiveData() {
        if (archivedAt != null) {
            throw new IllegalArgumentException("archivedAt must be null in current charge cycle status.");
        }

        if (archivedByUserId != null) {
            throw new IllegalArgumentException("archivedByUserId must be null in current charge cycle status.");
        }
    }

    private boolean hasCancellationData() {
        return canceledAt != null && canceledByUserId != null;
    }

    private boolean hasFinishData() {
        return finishedAt != null && finishedByUserId != null;
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

    private static void validateOptionalId(Long id, String fieldName) {
        if (id == null) {
            return;
        }

        if (id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be greater than zero.");
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