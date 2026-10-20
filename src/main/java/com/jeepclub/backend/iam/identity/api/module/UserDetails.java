package com.jeepclub.backend.iam.identity.api.module;

import java.time.Instant;
import java.time.LocalDate;

public record UserDetails(
        Long id,
        String name,
        LocalDate birthDate,
        String email,
        String cpf,
        String rg,
        String phoneNumber,
        String profilePhotoStorageKey,
        boolean administrativelyActive,
        Instant createdAt,
        Instant disabledAt,
        Instant updatedAt,
        UserStatus status,
        Instant archivedAt
) {
    /** Compatibility constructor for consumers that only need operational eligibility. */
    public UserDetails(Long id, String name, LocalDate birthDate, String email, String cpf, String rg,
                       String phoneNumber, String profilePhotoStorageKey, boolean administrativelyActive,
                       Instant createdAt, Instant disabledAt, Instant updatedAt) {
        this(id, name, birthDate, email, cpf, rg, phoneNumber, profilePhotoStorageKey, administrativelyActive,
                createdAt, disabledAt, updatedAt, administrativelyActive ? UserStatus.ACTIVE : UserStatus.INACTIVE, null);
    }
}
