package com.jeepclub.backend.dependents.core.domain.model;

import com.jeepclub.backend.dependents.core.domain.enums.DependentStatus;
import com.jeepclub.backend.dependents.core.domain.enums.RelationshipType;
import com.jeepclub.backend.dependents.core.domain.exception.DependentAlreadyDeletedException;
import com.jeepclub.backend.dependents.core.domain.exception.DependentException;
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
    private Long socioId;

    private DependentStatus status;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public static Dependent create(
            String name,
            String cpf,
            LocalDate birthDate,
            RelationshipType relationshipType,
            String phoneNumber,
            Long socioId,
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

        validateSocioId(socioId);
        validateNow(now);

        Dependent dependent = new Dependent();

        dependent.name = name.trim();
        dependent.cpf = normalizedCpf;
        dependent.birthDate = birthDate;
        dependent.relationshipType = relationshipType;
        dependent.phoneNumber = normalizePhoneNumber(phoneNumber);
        dependent.socioId = socioId;

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
            Long socioId,
            DependentStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
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

        validateSocioId(socioId);

        Objects.requireNonNull(
                status,
                "status cannot be null"
        );

        Objects.requireNonNull(
                createdAt,
                "createdAt cannot be null"
        );

        validateReconstitutedState(
                status,
                createdAt,
                updatedAt,
                deletedAt
        );

        Dependent dependent = new Dependent();

        dependent.id = id;
        dependent.name = name.trim();
        dependent.cpf = normalizedCpf;
        dependent.birthDate = birthDate;
        dependent.relationshipType = relationshipType;
        dependent.phoneNumber = normalizePhoneNumber(phoneNumber);
        dependent.socioId = socioId;

        dependent.status = status;

        dependent.createdAt = createdAt;
        dependent.updatedAt = updatedAt;
        dependent.deletedAt = deletedAt;

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

    public void selfDelete(Instant now) {
        validateNow(now);

        if (isDeleted()) {
            throw new DependentAlreadyDeletedException(id);
        }

        this.status = DependentStatus.DELETED;
        this.deletedAt = now;
        this.updatedAt = now;
    }

    public boolean isActive() {
        return status == DependentStatus.ACTIVE;
    }

    public boolean isDeleted() {
        return status == DependentStatus.DELETED;
    }

    private void assertActive() {
        if (!isActive()) {
            throw new DependentException(
                    "Deleted dependent cannot be modified."
            );
        }
    }

    private static void validateReconstitutedState(
            DependentStatus status,
            Instant createdAt,
            Instant updatedAt,
            Instant deletedAt
    ) {
        if (updatedAt != null && updatedAt.isBefore(createdAt)) {
            throw new DependentException(
                    "updatedAt cannot be before createdAt."
            );
        }

        if (status == DependentStatus.ACTIVE && deletedAt != null) {
            throw new DependentException(
                    "Active dependent cannot have deletedAt."
            );
        }

        if (status == DependentStatus.DELETED && deletedAt == null) {
            throw new DependentException(
                    "Deleted dependent must have deletedAt."
            );
        }

        if (deletedAt != null && deletedAt.isBefore(createdAt)) {
            throw new DependentException(
                    "deletedAt cannot be before createdAt."
            );
        }

        if (status == DependentStatus.DELETED
                && updatedAt == null) {
            throw new DependentException(
                    "Deleted dependent must have updatedAt."
            );
        }

        if (deletedAt != null
                && updatedAt != null
                && updatedAt.isBefore(deletedAt)) {

            throw new DependentException(
                    "updatedAt cannot be before deletedAt."
            );
        }
    }

    private static void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new DependentException(
                    "id must be positive."
            );
        }
    }

    private static void validateSocioId(Long socioId) {
        if (socioId == null || socioId <= 0) {
            throw new DependentException(
                    "socioId must be positive."
            );
        }
    }

    private static void requireText(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw new DependentException(
                    field + " is required."
            );
        }
    }

    private static String normalizeCpf(String rawCpf) {
        requireText(rawCpf, "cpf");

        String cpf = rawCpf.replaceAll("\\D", "");

        if (cpf.length() != 11) {
            throw new DependentException(
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
            throw new DependentException(
                    "now is required."
            );
        }
    }
}