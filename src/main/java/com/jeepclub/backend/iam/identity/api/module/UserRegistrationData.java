package com.jeepclub.backend.iam.identity.api.module;

import java.time.Instant;
import java.time.LocalDate;

public record UserRegistrationData(
        String name,
        LocalDate birthDate,
        String email,
        String cpf,
        String rg,
        String phoneNumber,
        String profilePhotoUrl,
        Instant now
) {
}
