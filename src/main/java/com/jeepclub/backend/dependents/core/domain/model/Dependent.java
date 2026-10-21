package com.jeepclub.backend.dependents.core.domain.model;

import com.jeepclub.backend.dependents.core.domain.exception.DependentLifecycleConflictException;
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
    private String photoStorageKey;
    private Long userId;

    private DependentStatus status;

    private Instant createdAt;
    private Instant updatedAt;
    private Instant archivedAt;

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
        return reconstitute(id, name, cpf, birthDate, relationshipType, phoneNumber, userId, status, createdAt, updatedAt, null);
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
            Instant updatedAt,
            Instant archivedAt
    ) {
        return reconstitute(id,name,cpf,birthDate,relationshipType,phoneNumber,userId,status,createdAt,updatedAt,archivedAt,null);
    }

    public static Dependent reconstitute(Long id, String name, String cpf, LocalDate birthDate,
            RelationshipType relationshipType, String phoneNumber, Long userId, DependentStatus status,
            Instant createdAt, Instant updatedAt, Instant archivedAt, String photoStorageKey) {
        if ((status == DependentStatus.ARCHIVED) != (archivedAt != null)) {
            throw new IllegalArgumentException("Archived state requires its archival timestamp only.");
        }
        if (archivedAt != null && (createdAt == null || archivedAt.isBefore(createdAt))) {
            throw new IllegalArgumentException("archivedAt cannot be before createdAt.");
        }

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

        dependent.archivedAt = archivedAt;
        dependent.photoStorageKey = photoStorageKey == null ? null : com.jeepclub.backend.shared.storage.PrivateImageReference.require(photoStorageKey);
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
        validateMutationTime(now);

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

    public boolean changePhoto(String storageKey, Instant now) {
        if (!isActive()) throw new DependentLifecycleConflictException("Only active dependents can change photos.");
        validateMutationTime(now);
        String reference = storageKey == null ? null : com.jeepclub.backend.shared.storage.PrivateImageReference.require(storageKey);
        if (Objects.equals(photoStorageKey, reference)) return false;
        photoStorageKey = reference;
        updatedAt = now;
        return true;
    }

    public void disable(Instant now) {
        assertNotArchived();
        validateMutationTime(now);

        if (isDisabled()) {
            return;
        }

        this.status = DependentStatus.INACTIVE;
        this.updatedAt = now;
    }

    public void enable(Instant now) {
        assertNotArchived();
        validateMutationTime(now);

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
        return status == DependentStatus.INACTIVE;
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

        String phoneNumber = rawPhoneNumber.replaceAll("\\D", "");

        if (phoneNumber.length() < 10 || phoneNumber.length() > 11) {
            throw new IllegalArgumentException(
                    "phoneNumber must contain 10 or 11 digits."
            );
        }

        return phoneNumber;
    }

    private static void validateNow(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException(
                    "now is required."
            );
        }
    }

    private void validateMutationTime(Instant now) {
        validateNow(now);
        if (now.isBefore(createdAt)) {
            throw new IllegalArgumentException("updatedAt cannot be before createdAt.");
        }
    }

    public boolean archive(Instant now) {
        validateMutationTime(now);
        if (status == DependentStatus.ARCHIVED) return false;
        status = DependentStatus.ARCHIVED;
        archivedAt = now;
        updatedAt = now;
        return true;
    }

    public boolean unarchive(Instant now) {
        validateMutationTime(now);
        if (status == DependentStatus.INACTIVE) return false;
        if (status != DependentStatus.ARCHIVED) {
            throw new DependentLifecycleConflictException("Only an archived record can be unarchived.");
        }
        status = DependentStatus.INACTIVE;
        archivedAt = null;
        updatedAt = now;
        return true;
    }

    private void assertNotArchived() {
        if (status == DependentStatus.ARCHIVED) {
            throw new DependentLifecycleConflictException("Archived record requires explicit unarchive before this operation.");
        }
    }
}
