package com.jeepclub.backend.memberships.core.domain.model;

import com.jeepclub.backend.memberships.core.domain.enums.MembershipApplicationStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MembershipApplication {

    private Long id;

    private String name;
    private String cpf;
    private String email;
    private String phoneNumber;
    private String message;

    private MembershipApplicationStatus status;
    private String rejectionReason;

    private Long reviewedByUserId;
    private Long createdUserId;

    private Instant requestedAt;
    private Instant reviewedAt;
    private Instant finishedAt;
    private Instant updatedAt;

    private Long version;

    public static MembershipApplication create(
            String name,
            String cpf,
            String email,
            String phoneNumber,
            String message,
            Instant now
    ) {
        Objects.requireNonNull(now, "now must not be null");

        MembershipApplication app = new MembershipApplication();
        app.name = requireText(name, "name");
        app.cpf = requireText(cpf, "cpf");
        app.email = normalizeNullable(email);
        app.phoneNumber = requireText(phoneNumber, "phoneNumber");
        app.message = normalizeNullable(message);
        app.status = MembershipApplicationStatus.PENDING;
        app.requestedAt = now;
        app.updatedAt = now;

        return app;
    }

    public static MembershipApplication reconstitute(
            Long id,
            String name,
            String cpf,
            String email,
            String phoneNumber,
            String message,
            MembershipApplicationStatus status,
            String rejectionReason,
            Long reviewedByUserId,
            Long createdUserId,
            Instant requestedAt,
            Instant reviewedAt,
            Instant finishedAt,
            Instant updatedAt,
            Long version
    ) {
        MembershipApplication app = new MembershipApplication();

        app.id = id;
        app.name = name;
        app.cpf = cpf;
        app.email = email;
        app.phoneNumber = phoneNumber;
        app.message = message;
        app.status = status;
        app.rejectionReason = rejectionReason;
        app.reviewedByUserId = reviewedByUserId;
        app.createdUserId = createdUserId;
        app.requestedAt = requestedAt;
        app.reviewedAt = reviewedAt;
        app.finishedAt = finishedAt;
        app.updatedAt = updatedAt;
        app.version = version;

        return app;
    }

    public void approve(
            Long reviewedByUserId,
            Long createdUserId,
            Instant now
    ) {
        ensureStatus(MembershipApplicationStatus.PENDING);
        Objects.requireNonNull(reviewedByUserId, "reviewedByUserId must not be null");
        Objects.requireNonNull(createdUserId, "createdUserId must not be null");
        Objects.requireNonNull(now, "now must not be null");

        this.status = MembershipApplicationStatus.APPROVED;
        this.reviewedByUserId = reviewedByUserId;
        this.createdUserId = createdUserId;
        this.reviewedAt = now;
        this.rejectionReason = null;
        this.updatedAt = now;
    }

    public void reject(
            Long reviewedByUserId,
            String rejectionReason,
            Instant now
    ) {
        ensureStatus(MembershipApplicationStatus.PENDING);
        Objects.requireNonNull(reviewedByUserId, "reviewedByUserId must not be null");
        Objects.requireNonNull(now, "now must not be null");

        this.status = MembershipApplicationStatus.REJECTED;
        this.reviewedByUserId = reviewedByUserId;
        this.rejectionReason = normalizeNullable(rejectionReason);
        this.reviewedAt = now;
        this.finishedAt = now;
        this.updatedAt = now;
    }

    public void complete(Instant now) {
        ensureStatus(MembershipApplicationStatus.APPROVED);
        Objects.requireNonNull(now, "now must not be null");

        this.status = MembershipApplicationStatus.COMPLETED;
        this.finishedAt = now;
        this.updatedAt = now;
    }

    private void ensureStatus(MembershipApplicationStatus expected) {
        if (this.status != expected) {
            throw new IllegalStateException(
                    "Invalid membership application transition: "
                            + this.status + " -> " + expected
            );
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank"
            );
        }

        return value.trim();
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank()
                ? null
                : value.trim();
    }
}
