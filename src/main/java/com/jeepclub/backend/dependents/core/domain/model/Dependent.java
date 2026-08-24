package com.jeepclub.backend.dependents.core.domain.model;

import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Dependent {

    private Long id;
    private String name;
    private String cpf;
    private LocalDate birthDate;
    private RelationshipType relationshipType;
    private String phoneNumber;
    private Long userId;

    private DependentStatus status;

    private Instant createdAt;
    private Instant updatedAt;

    public static Dependent create(
            String name,
            String cpf,
            LocalDate birthDate,
            RelationshipType relationshipType,
            String phoneNumber,
            Long userId,
            Instant now
    ) {
        requireText(name, "name");

        String normalizedCpf = normalizeCpf(cpf);

        Objects.requireNonNull(
                birthDate,
                "birthDate cannot be null"
        );

        Objects.requireNonNull(
                relationshipType,
                "relationshipType cannot be null"
        );

        validateUserId(userId);
        validateNow(now);

        Dependent dependent = new Dependent();

        dependent.name = name.trim();
        dependent.cpf = normalizedCpf;
        dependent.birthDate = birthDate;
        dependent.relationshipType = relationshipType;
        dependent.phoneNumber = normalizePhoneNumber(phoneNumber);
        dependent.userId = userId;

        dependent.status = DependentStatus.ACTIVE;

        dependent.createdAt = now;

        return dependent;
    }

    public static Dependent reconstitute(
            Long id,
            String name,
            String cpf,
            LocalDate birthDate,
            RelationshipType relationshipType,
            String phoneNumber,
            Long userId,
            DependentStatus status,
            Instant createdAt,
            Instant updatedAt
    ) {
        validateId(id);
        requireText(name, "name");

        String normalizedCpf = normalizeCpf(cpf);

        Objects.requireNonNull(
                birthDate,
                "birthDate cannot be null"
        );

        Objects.requireNonNull(
                relationshipType,
                "relationshipType cannot be null"
        );

        validateUserId(userId);

        Objects.requireNonNull(
                status,
                "status cannot be null"
        );

        Objects.requireNonNull(
                createdAt,
                "createdAt cannot be null"
        );

        validateReconstitutedState(
                createdAt,
                updatedAt
        );

        Dependent dependent = new Dependent();

        dependent.id = id;
        dependent.name = name.trim();
        dependent.cpf = normalizedCpf;
        dependent.birthDate = birthDate;
        dependent.relationshipType = relationshipType;
        dependent.phoneNumber = normalizePhoneNumber(phoneNumber);
        dependent.userId = userId;

        dependent.status = status;

        dependent.createdAt = createdAt;
        dependent.updatedAt = updatedAt;

        return dependent;
    }

    public void update(
            String name,
            String cpf,
            LocalDate birthDate,
            RelationshipType relationshipType,
            String phoneNumber,
            Instant now
    ) {
        assertActive();
        validateNow(now);

        requireText(name, "name");

        Objects.requireNonNull(
                birthDate,
                "birthDate cannot be null"
        );

        Objects.requireNonNull(
                relationshipType,
                "relationshipType cannot be null"
        );

        this.name = name.trim();
        this.cpf = normalizeCpf(cpf);
        this.birthDate = birthDate;
        this.relationshipType = relationshipType;
        this.phoneNumber = normalizePhoneNumber(phoneNumber);
        this.updatedAt = now;
    }

    public void disable(Instant now) {
        validateNow(now);

        if (isDisabled()) {
            return;
        }

        this.status = DependentStatus.DISABLED;
        this.updatedAt = now;
    }

    public void enable(Instant now) {
        validateNow(now);

        if (isActive()) {
            return;
        }

        this.status = DependentStatus.ACTIVE;
        this.updatedAt = now;
    }

    public boolean isActive() {
        return status == DependentStatus.ACTIVE;
    }

    public boolean isDisabled() {
        return status == DependentStatus.DISABLED;
    }

    private void assertActive() {
        if (!isActive()) {
            throw new IllegalStateException(
                    "Dependent must be active."
            );
        }
    }

    private static void validateReconstitutedState(
            Instant createdAt,
            Instant updatedAt
    ) {
        if (updatedAt != null
                && updatedAt.isBefore(createdAt)) {

            throw new IllegalStateException(
                    "updatedAt cannot be before createdAt."
            );
        }
    }

    private static void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "id must be positive."
            );
        }
    }

    private static void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException(
                    "userId must be positive."
            );
        }
    }

    private static void requireText(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " is required."
            );
        }
    }

    private static String normalizeCpf(String rawCpf) {
        requireText(rawCpf, "cpf");

        String cpf = rawCpf.replaceAll("\\D", "");

        if (cpf.length() != 11) {
            throw new IllegalArgumentException(
                    "cpf must contain exactly 11 digits."
            );
        }

        return cpf;
    }

    private static String normalizePhoneNumber(
            String rawPhoneNumber
    ) {
        if (rawPhoneNumber == null || rawPhoneNumber.isBlank()) {
            return null;
        }

        return rawPhoneNumber.replaceAll("\\D", "");
    }

    private static void validateNow(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException(
                    "now is required."
            );
        }
    }
}