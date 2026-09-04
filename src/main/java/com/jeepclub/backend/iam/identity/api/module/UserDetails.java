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
        String profilePhotoUrl,
        boolean administrativelyActive,
        Instant createdAt,
        Instant disabledAt,
        Instant updatedAt
) {
}
